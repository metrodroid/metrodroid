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

import android.os.Parcelable
import android.support.annotation.StringRes
import android.support.annotation.VisibleForTesting
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
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.TripObfuscator
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import java.util.*

@Parcelize
internal data class RkfSerial(val mCompany: Int, val mCustomerNumber: Long, val mHwSerial: Long) : Parcelable {
    val formatted: String
        get() = when (mCompany) {
            RkfLookup.REJSEKORT -> {
                val main = "30843" + NumberUtils.formatNumber(mCustomerNumber, " ", 1, 3, 3, 3)
                main + " " + NumberUtils.calculateLuhn(main.replace(" ", ""))
            }
            RkfLookup.SLACCESS -> {
                NumberUtils.formatNumber(mHwSerial, " ", 5, 5)
            }
            else -> mHwSerial.toString()
        }
}

// Specification: https://github.com/mchro/RejsekortReader/tree/master/resekortsforeningen
@Parcelize
data class RkfTransitData internal constructor(
        private val mTcci: En1545Parsed,
        private val mTrips: List<Trip>,
        private val mBalances: List<RkfPurse>,
        private val mLookup: RkfLookup,
        private val mTccps: List<En1545Parsed>,
        private val mSerial: RkfSerial) : TransitData() {
    override val cardName get(): String = issuerMap[aid]?.name ?: "RKF"

    private val aid
        get() = mTcci.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID)

    override val serialNumber get() = mSerial.formatted

    override val trips get() = mTrips

    // Filter out ghost purse on Rejsekort unless it was ever used (is it ever?)
    override val balances get() = mBalances.withIndex().filter { (idx, bal) ->
        aid != RkfLookup.REJSEKORT
                || idx != 1 || bal.transactionNumber != 0
    }
            .map { (idx, bal) -> bal.balance }

    @VisibleForTesting
    val issuer: String
        get() = mLookup.getAgencyName(mTcci.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID), false)

    @VisibleForTesting
    val expiryDate: Calendar?
        get() = mTcci.getTimeStamp(En1545TransitData.ENV_APPLICATION_VALIDITY_END, mLookup.timeZone)

    @VisibleForTesting
    val cardStatus
        @StringRes
        get() = when (mTcci.getIntOrZero(STATUS)) {
            0x01 -> R.string.rkf_status_ok
            0x21 -> R.string.rkf_status_action_pending
            0x3f -> R.string.rkf_status_temp_disabled
            0x58 -> R.string.rkf_status_not_ok
            else -> R.string.unknown_format
        }

    override val info get() = listOf(
            ListItem(R.string.expiry_date,
                    Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(expiryDate))),
            ListItem(R.string.card_issuer, issuer),
            if (cardStatus == R.string.unknown_format) {
                ListItem(R.string.rkf_card_status, Utils.localizeString(R.string.unknown_format,
                        NumberUtils.intToHex(mTcci.getIntOrZero(STATUS))))
            } else {
                ListItem(R.string.rkf_card_status, cardStatus)
            })

    companion object {
        private val issuerMap = mapOf(
                RkfLookup.SLACCESS to CardInfo.Builder()
                        .setName("SLaccess")
                        .setLocation(R.string.location_stockholm)
                        .setCardType(CardType.MifareClassic)
                        .setKeysRequired()
                        .setPreview()
                        .build(),
                RkfLookup.REJSEKORT to CardInfo.Builder()
                        .setName("Rejsekort")
                        .setLocation(R.string.location_denmark)
                        .setCardType(CardType.MifareClassic)
                        .setKeysRequired()
                        .setPreview()
                        .build()
        )

        val FACTORY = object : ClassicCardTransitFactory {
            override fun earlyCardInfo(sectors: List<ClassicSector>) = issuerMap[getIssuer(sectors[0])]

            override fun earlyCheck(sectors: List<ClassicSector>) =
                    Utils.checkKeyHash(sectors[0].key, "rkf",
                            // Most cards
                            "b9ae9b2f6855aa199b4af7bdc130ba1c",
                            "2107bb612627fb1dfe57348fea8a8b58",
                            // Jo-jo
                            "f40bb9394d94c7040c1dd19997b4f5e8") >= 0

            override fun earlySectors() = 1

            override fun getAllCards() = issuerMap.values.toList()

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
                val serial = getSerial(card)
                val issuerName = issuerMap[serial.mCompany]?.name ?: "RKF"
                return TransitIdentity(issuerName, serial.formatted)
            }

            override fun parseTransitData(card: ClassicCard): RkfTransitData {
                val tcciRaw = card[0, 1].data
                val tcci = En1545Parser.parseLeBits(tcciRaw, 0, TCCI_FIELDS)
                val tripVersion = tcci.getIntOrZero(EVENT_LOG_VERSION)
                val currency = tcci.getIntOrZero(CURRENCY)
                val company = tcci.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID)
                val lookup = RkfLookup(currency, company)
                val transactions = mutableListOf<RkfTransaction>()
                val balances = mutableListOf<RkfPurse>()
                val tccps = mutableListOf<En1545Parsed>()
                val unfilteredTrips = mutableListOf<RkfTCSTTrip>()
                recordloop@ for (record in getRecords(card))
                    when (record[0].toInt() and 0xff) {
                        0x84 -> transactions += RkfTransaction.parseTransaction(record, lookup, tripVersion) ?: continue@recordloop
                        0x85 -> balances += RkfPurse.parse(record, lookup)
                        0xa2 -> tccps += En1545Parser.parseLeBits(record, TCCP_FIELDS)
                        0xa3 -> unfilteredTrips += RkfTCSTTrip.parse(record, lookup) ?: continue@recordloop
                    }
                transactions.sortBy { it.timestamp.timeInMillis }
                unfilteredTrips.sortBy { it.startTimestamp.timeInMillis }
                val trips = mutableListOf<RkfTCSTTrip>()
                // Check if unfinished trip is superseeded by finished one
                for ((idx, trip) in unfilteredTrips.withIndex()) {
                    if (idx > 0 && unfilteredTrips[idx - 1].startTimestamp.timeInMillis == trip.startTimestamp.timeInMillis
                            && unfilteredTrips[idx - 1].checkoutCompleted && !trip.checkoutCompleted)
                        continue
                    if (idx < unfilteredTrips.size - 1 && unfilteredTrips[idx + 1].startTimestamp.timeInMillis == trip.startTimestamp.timeInMillis
                            && unfilteredTrips[idx + 1].checkoutCompleted && !trip.checkoutCompleted)
                        continue
                    trips.add(trip)
                }
                val nonTripTransactions = transactions.filter { it.isOther() }
                val tripTransactions = transactions.filter { !it.isOther() }
                val remainingTransactions = mutableListOf<RkfTransaction>()
                var i = 0
                for (trip in trips)
                    while (i < tripTransactions.size) {
                        val transaction = tripTransactions[i]
                        val transactionTimestamp = clearSeconds(transaction.timestamp.timeInMillis)
                        if (transactionTimestamp > clearSeconds(trip.endTimestamp.timeInMillis))
                            break
                        i++
                        if (transactionTimestamp < clearSeconds(trip.startTimestamp.timeInMillis)) {
                            remainingTransactions.add(transaction)
                            continue
                        }
                        trip.addTransaction(transaction)
                    }
                if (i < tripTransactions.size)
                    remainingTransactions.addAll(tripTransactions.subList(i, tripTransactions.size))
                return RkfTransitData(mTcci = tcci,
                        mTrips = TransactionTrip.merge(nonTripTransactions + remainingTransactions)
                                + trips.map { it.tripLegs }.flatten(),
                        mBalances = balances, mLookup = lookup, mTccps = tccps, mSerial = getSerial(card))
            }
        }

        internal fun clearSeconds(timeInMillis: Long) = timeInMillis / 60000 * 60000

        private fun getRecords(card: ClassicCard): List<ImmutableByteArray> {
            val records = mutableListOf<ImmutableByteArray>()
            var sector = 3
            var block = 0

            while (sector < card.sectors.size) {
                // FIXME: we should also check TCDI entry but TCDI doesn't match the spec apparently,
                // so for now just use id byte
                val type = card[sector, block].data.getBitsFromBufferLeBits(0, 8)
                if (type == 0) {
                    sector++
                    block = 0
                    continue
                }
                var first = true
                val oldSector = sector
                var oldBlockCount = -1

                while (sector < card.sectors.size && (first || block != 0)) {
                    first = false
                    val blockData = card[sector, block].data
                    val newType = blockData.getBitsFromBufferLeBits(0, 8)
                    // Some Rejsekort skip slot in the middle of the sector
                    if (newType == 0 && block + oldBlockCount < card[sector].blocks.size - 1) {
                        block += oldBlockCount
                        continue
                    }
                    if (newType != type)
                        break
                    val version = blockData.getBitsFromBufferLeBits(8, 6)
                    val blockCount = getBlockCount(type, version)
                    if (blockCount == -1) {
                        break
                    }
                    oldBlockCount = blockCount
                    var dat = ImmutableByteArray(0)

                    repeat(blockCount) {
                        dat += card[sector, block].data
                        block++
                        if (block >= card[sector].blocks.size - 1) {
                            sector++
                            block = 0
                        }
                    }

                    records += dat
                }
                if (block != 0 || sector == oldSector) {
                    sector++
                    block = 0
                }
            }
            return records
        }

        private fun getBlockCount(type: Int, version: Int) = when (type) {
            0x84 -> 1
            0x85 -> when (version) {
                // Only 3 is tested
                1, 2, 3, 4, 5 -> 3
                else -> 6
            }
            0xa2 -> 2
            0xa3 -> when (version) {
                // Only 2 is tested
                1, 2 -> 3
                // Only 5 is tested
                // 3 seems already have size 6
                else -> 6
            }
            else -> -1
        }

        private fun getSerial(card: ClassicCard): RkfSerial {
            val issuer = getIssuer(card[0])

            val hwSerial = card[0, 0].data.byteArrayToLongReversed(0, 4)

            for (record in getRecords(card))
                if ((record[0].toInt() and 0xff) == 0xa2) {
                    val low = record.getBitsFromBufferLeBits(34, 20).toLong()
                    val high = record.getBitsFromBufferLeBits(54, 14).toLong()
                    return RkfSerial(mCompany = issuer, mHwSerial = hwSerial, mCustomerNumber = (high shl 20) or low)
                }
            return RkfSerial(mCompany = issuer, mHwSerial = hwSerial, mCustomerNumber = 0)
        }

        private fun getIssuer(sector0: ClassicSector) = sector0[1].data.getBitsFromBufferLeBits(22, 12)

        internal const val COMPANY = "Company"
        internal const val STATUS = "Status"
        internal val HEADER = En1545Container(
                En1545FixedInteger("Identifier", 8),
                En1545FixedInteger("Version", 6),
                En1545FixedInteger(COMPANY, 12)
        )

        internal val STATUS_FIELD = En1545FixedInteger(STATUS, 8)
        internal val MAC = En1545Container(
                En1545FixedInteger("MACAlgorithmIdentifier", 2),
                En1545FixedInteger("MACKeyIdentifier", 6),
                En1545FixedInteger("MACAuthenticator", 16)
        )

        private const val CURRENCY = "CardCurrencyUnit"
        private const val EVENT_LOG_VERSION = "EventLogVersionNumber"
        private val TCCI_FIELDS = En1545Container(
                En1545FixedInteger("MADindicator", 16),
                En1545FixedInteger("CardVersion", 6),
                En1545FixedInteger(En1545TransitData.ENV_APPLICATION_ISSUER_ID, 12),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                STATUS_FIELD,
                En1545FixedInteger(CURRENCY, 16),
                En1545FixedInteger(EVENT_LOG_VERSION, 6),
                En1545FixedInteger("A", 26),
                MAC
        )
        private val TCCP_FIELDS = En1545Container(
                HEADER,
                STATUS_FIELD,
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