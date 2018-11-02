/*
 * RkfTransitData.kt
 *
 * Copyright 2018 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.transit.rkf

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import kotlinx.android.parcel.Parcelize
import au.id.micolous.metrodroid.transit.en1545.En1545TransitData.ENV_APPLICATION_ISSUER_ID
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger
import au.id.micolous.metrodroid.transit.en1545.En1545TransitData.ENV_APPLICATION_VALIDITY_END
import java.util.*

// Specification: https://github.com/mchro/RejsekortReader/tree/master/resekortsforeningen
@Parcelize
data class RkfTransitData (val mTcci: En1545Parsed, val mTrips: List<TransactionTrip>,
                           val mBalances: List<TransitBalanceStored>,
                           val mLookup: RkfLookup,
                           val mTccps: List<En1545Parsed>,
                           val mSerial: Long) : TransitData() {

    override fun getCardName(): String = issuerMap[getCardAID()]?.name ?: "RKF"

    private fun getCardAID(): Int = mTcci.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID)

    override fun getSerialNumber() = formatSerial(mSerial, getCardAID())

    override fun getTrips() = mTrips.toTypedArray()

    override fun getBalances() = mBalances

    override fun getInfo(): List<ListItem> {
        val li = mutableListOf<ListItem>()
        li.add(ListItem(R.string.expiry_date,
                mTcci.getTimeStampString(ENV_APPLICATION_VALIDITY_END, mLookup.timeZone)))
        li.add(ListItem(R.string.card_issuer,
                mLookup.getAgencyName(mTcci.getIntOrZero(ENV_APPLICATION_ISSUER_ID), false)))
        return li
    }

    companion object {
        private val issuerMap = hashMapOf(
                RkfLookup.REJSEKORT to CardInfo.Builder()
                        .setName("Rejsekort")
                        .setLocation(R.string.location_denmark)
                        .setCardType(CardType.MifareClassic)
                        .setKeysRequired()
                        .setPreview()
                        .build()
        )

        fun getIssuer(sector0: ClassicSector) : Int {
            val tcci = sector0.getBlock(1).data
            return Utils.getBitsFromBufferLeBits(tcci, 22, 12)
        }

        val FACTORY = object : ClassicCardTransitFactory() {
            override fun earlyCardInfo(sectors: MutableList<ClassicSector>): CardInfo? {
                val sector0 = sectors[0]
                if (!check(sector0))
                    return null
                val issuer = getIssuer(sector0)
                return issuerMap[issuer]
            }

            private fun check(sector0: ClassicSector): Boolean {
                try {
                    val key = sector0.key

                    return Utils.checkKeyHash(key, "rkf", "b9ae9b2f6855aa199b4af7bdc130ba1c",
                            "2107bb612627fb1dfe57348fea8a8b58") >= 0
                } catch (ignored: IndexOutOfBoundsException) {
                    // If that sector number is too high, then it's not for us.
                }

                return false
            }

            override fun check(card: ClassicCard): Boolean {
                try {
                    return check(card.getSector(4))
                } catch (ignored: IndexOutOfBoundsException) {
                    // If that sector number is too high, then it's not for us.
                }

                return false
            }

            override fun parseTransitIdentity(card: ClassicCard) : TransitIdentity {
                val sector0 = card.getSector(0)
                val serial = getSerial(card)
                val issuer = getIssuer(sector0)
                val issuerName = issuerMap[issuer]?.name ?: "RKF"
                return TransitIdentity(issuerName, formatSerial(serial, issuer))
            }

            override fun parseTransitData(card: ClassicCard) : RkfTransitData {
                val tcciRaw = card.getSector(0).getBlock(1).data
                val tcci = En1545Parser.parseLeBits(tcciRaw, 0, TCCI_FIELDS)
                val tripVersion = tcci.getIntOrZero(EVENT_LOG_VERSION)
                val currency = tcci.getIntOrZero(CURRENCY_FIELD)
                val company = tcci.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID)
                val lookup = RkfLookup(currency, company)
                val trips = mutableListOf<En1545Transaction>()
                val balances = mutableListOf<TransitBalanceStored>()
                val tccps = mutableListOf<En1545Parsed>()
                var sectorNo = 3

                while (sectorNo <= 14) {
                    val sector = card.getSector(sectorNo)
                    val id = sector.getBlock(0).data[0].toInt() and 0xff
                    // FIXME: we should also check TCDI entry but TCDI doesn't match the spec apparently,
                    // so for now just use id byte
                    when(id) {
                        0x84 -> for (block in 0..2) {
                            val data = sector.getBlock(block).data
                            if ((data[0].toInt() and 0xff) != 0x84)
                                    continue
                            trips.add(RkfTransaction.parseTransaction(data, lookup, tripVersion))
                        }
                        0x85 -> {
                            val static = En1545Parser.parseLeBits(sector.getBlock(0).data, TCPU_STATIC_FIELDS)
                            val block1 = card.getSector(sectorNo + 1).getBlock(1).data
                            val block2 = sector.getBlock(2).data
                            val block = if (Utils.getBitsFromBufferLeBits(block1, 0, 16)
                                    > Utils.getBitsFromBufferLeBits(block2, 0, 16)) block1 else block2
                            val dynamic = En1545Parser.parseLeBits(block, TCPU_DYNAMIC_FIELDS)
                            val balance = lookup.parseCurrency(dynamic.getIntOrZero("Value"))
                            val name = lookup.getAgencyName(static.getIntOrZero("Company"), true)

                            // Skip empty purses of the same company
                            if (balances.isEmpty() || Utils.getBitsFromBufferLeBits(block, 0, 16) != 0
                                || balances[0].name != name)
                                balances.add(TransitBalanceStored(balance, name, null))
                            // Skip next sector
                            sectorNo++
                        }

                        0xa2 -> {
                            tccps.add(En1545Parser.parseLeBits(sector.readBlocks(0, 2), TCCP_FIELDS))
                            val block = sector.getBlock(2).data
                            if (block[0].toInt() and 0xff == 0xa2) {
                                sectorNo++
                                val sector2 = card.getSector(sectorNo)
                                val block2 = sector2.getBlock(0).data
                                tccps.add(En1545Parser.parseLeBits(Utils.concatByteArrays(block, block2), TCCP_FIELDS))
                                if (sector2.getBlock(1).data[0].toInt() and 0xff == 0xa2)
                                    tccps.add(En1545Parser.parseLeBits(sector2.readBlocks(1, 2), TCCP_FIELDS))
                            }
                        }
                    }
                    sectorNo++
                }
                return RkfTransitData(mTcci = tcci, mTrips = TransactionTrip.merge(trips) { el -> RkfTrip(el!!) },
                        mBalances = balances, mLookup = lookup, mTccps = tccps, mSerial = getSerial(card))
            }
        }

        private fun getSerial(card: ClassicCard): Long {
            var sectorNo = 3

            while (sectorNo <= 14) {
                val sector = card.getSector(sectorNo)
                val id = sector.getBlock(0).data[0].toInt() and 0xff
                when(id) {
                    0x84 -> {}
                    0x85 -> {
                        // Skip next sector
                        sectorNo++
                    }

                    0xa2 -> {
                        val low = Utils.getBitsFromBufferLeBits(sector.getBlock(0).data, 34, 20).toLong()
                        val high = Utils.getBitsFromBufferLeBits(sector.getBlock(0).data, 54, 14).toLong()
                        return (high shl 20) or low
                    }
                }
                sectorNo++
            }
            return 0
        }

        private fun formatSerial(serial: Long, company: Int): String {
            when (company) {
                RkfLookup.REJSEKORT -> {
                    val main = "30843%010d".format(Locale.ENGLISH, serial)
                    val full = main + Utils.calculateLuhn(main)
                    return (full.substring(0, 6) + " " + full.substring(6, 9) + " "
                            + full.substring(9, 12) + " " + full.substring(12, 15)
                            + " " + full.substring(15))
                }
                else -> return serial.toString()
            }
        }

        internal val IDENTIFIER = En1545FixedInteger("Identifier", 8)
        val VERSION = En1545FixedInteger("Version", 6)
        private val COMPANY = En1545FixedInteger("Company", 12)
        private val MAC = En1545Container(
                En1545FixedInteger("MACAlgorithmIdentifier", 2),
                En1545FixedInteger("MACKeyIdentifier", 6),
                En1545FixedInteger("MACAuthenticator", 16)
        )

        private const val CURRENCY_FIELD = "CardCurrencyUnit"
        private const val EVENT_LOG_VERSION = "EventLogVersionNumber"
        private val TCCI_FIELDS = En1545Container(
                En1545FixedInteger("MADindicator", 16),
                En1545FixedInteger("CardVersion", 6),
                En1545FixedInteger(En1545TransitData.ENV_APPLICATION_ISSUER_ID, 12),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                En1545FixedInteger("CardStatus", 8),
                En1545FixedInteger(CURRENCY_FIELD, 16),
                En1545FixedInteger(EVENT_LOG_VERSION, 6),
                MAC
        )
        private val TCPU_STATIC_FIELDS = En1545Container(
                IDENTIFIER,
                VERSION,
                COMPANY,
                En1545FixedInteger("PurseSerialNumber", 32),
                En1545FixedInteger.date("Start"),
                En1545FixedInteger("DataPointer", 4),
                En1545FixedInteger("MinimumValue", 24),
                En1545FixedInteger("AutoLoadValue", 24)
        )
        private val TCPU_DYNAMIC_FIELDS = En1545Container(
                En1545FixedInteger("PurseTransactionNumber", 16),
                En1545FixedInteger("Value", 24)
                // Rest unknown
        )
        private val TCCP_FIELDS = En1545Container(
                IDENTIFIER,
                VERSION,
                COMPANY,
                En1545FixedInteger("Status", 8),
                En1545Container(
                        // This is actually a single field. Split is only
                        // because of limitations of parser
                        En1545FixedInteger("CustomerNumberLow", 20),
                        En1545FixedInteger("CustomerNumberHigh", 14)
                )
                // Rest unknown
        )
    }
}