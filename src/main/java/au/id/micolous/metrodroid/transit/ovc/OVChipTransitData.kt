/*
 * OVChipTransitData.java
 *
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.ovc

import java.util.Arrays
import java.util.Calendar

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.InvalidClassicSector
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransactionTrip
import au.id.micolous.metrodroid.transit.TransitBalanceStored
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OVChipTransitData(
        private val parsed: En1545Parsed,
        private val mIndex: OVChipIndex,
        private val mExpdate: Int,
        private val mType: Int,
        private val mCreditSlotId: Int,
        private val mCreditId: Int,
        private val mCredit: Int,
        private val mBanbits: Int,
        private val mTrips: List<TransactionTrip>,
        private val mSubscriptions: List<OVChipSubscription>
) : En1545TransitData(parsed) {
    override fun getCardName() = NAME

    public override fun getBalance() =
            TransitBalanceStored(TransitCurrency.EUR(mCredit),
                    Utils.localizeString(if (mType == 2) R.string.card_type_personal else R.string.card_type_anonymous),
                    OVChipTransitData.convertDate(mExpdate))

    override fun getSerialNumber() = null

    override fun getTrips() = mTrips

    override fun getSubscriptions() = mSubscriptions

    override fun getLookup() = OvcLookup.instance

    override fun getInfo() = super.getInfo().orEmpty() + listOf(
            ListItem("Banned", if (mBanbits and 0xC0 == 0xC0) "Yes" else "No"),

            HeaderListItem(R.string.credit_information),
            ListItem("Credit Slot ID", Integer.toString(mCreditSlotId)),
            ListItem("Last Credit ID", Integer.toString(mCreditId)),
            ListItem(R.string.ovc_autocharge,
                    if (mTicketEnvParsed.getIntOrZero(AUTOCHARGE_ACTIVE) == 0x05) "Yes" else "No"),
            ListItem(R.string.ovc_autocharge_limit,
                    TransitCurrency.EUR(mTicketEnvParsed.getIntOrZero(AUTOCHARGE_LIMIT))
                            .maybeObfuscateBalance().formatCurrencyString(true)),
            ListItem(R.string.ovc_autocharge_amount,
                    TransitCurrency.EUR(mTicketEnvParsed.getIntOrZero(AUTOCHARGE_CHARGE))
                            .maybeObfuscateBalance().formatCurrencyString(true)),

            HeaderListItem("Recent Slots"),
            ListItem("Transaction Slot", if (mIndex.recentTransactionSlot) "B" else "A"),
            ListItem("Info Slot", if (mIndex.recentInfoSlot) "B" else "A"),
            ListItem("Subscription Slot", if (mIndex.recentSubscriptionSlot) "B" else "A"),
            ListItem("Travelhistory Slot", if (mIndex.recentTravelhistorySlot) "B" else "A"),
            ListItem("Credit Slot", if (mIndex.recentCreditSlot) "B" else "A"))

    companion object {
        private const val NAME = "OV-chipkaart"
        private val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.ovchip_card)
                .setName(NAME)
                .setLocation(R.string.location_the_netherlands)
                .setCardType(CardType.MifareClassic)
                .setKeysRequired()
                .build()

        private val OVC_HEADER = byteArrayOf(0x84.toByte(), 0, 0, 0, 0x06, 0x03, 0xa0.toByte(), 0, 0x13, 0xae.toByte(), 0xe4.toByte())
        private const val AUTOCHARGE_ACTIVE = "AutochargeActive"
        private const val AUTOCHARGE_LIMIT = "AutochargeLimit"
        private const val AUTOCHARGE_CHARGE = "AutochargeCharge"
        private const val AUTOCHARGE_UNKNOWN = "AutochargeUnknown"

        fun parse(card: ClassicCard): OVChipTransitData {
            val index = OVChipIndex.parse(card[39].readBlocks(11, 4))
            val credit = card[39].readBlocks(if (index.recentCreditSlot) 10 else 9, 1)
            val mTicketEnvParsed = En1545Parser.parse(
                    card[if (index.recentInfoSlot) 23 else 22].readBlocks(0, 3),
                    En1545Container(
                            En1545FixedHex("EnvUnknown1", 48),
                            En1545FixedInteger(En1545TransitData.ENV_APPLICATION_ISSUER_ID, 5), // Could be 4 bits though
                            En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                            En1545FixedHex("EnvUnknown2", 43),
                            En1545Bitmap(
                                    En1545FixedHex("NeverSeen1", 8),
                                    En1545Container(
                                            En1545FixedInteger(En1545TransitData.HOLDER_BIRTH_DATE, 32),
                                            En1545FixedHex("EnvUnknown3", 32),
                                            En1545FixedInteger(AUTOCHARGE_ACTIVE, 3),
                                            En1545FixedInteger(AUTOCHARGE_LIMIT, 16),
                                            En1545FixedInteger(AUTOCHARGE_CHARGE, 16),
                                            En1545FixedInteger(AUTOCHARGE_UNKNOWN, 16)
                                    )
                            )
                    ))

            return OVChipTransitData(parsed = mTicketEnvParsed, mIndex = index,
                    //byte 0-11:unknown const
                    mExpdate = Utils.getBitsFromBuffer(card[0, 1].data, 88, 20),
                    // last bytes: unknown const
                    mBanbits = Utils.getBitsFromBuffer(credit, 0, 9),
                    mCreditSlotId = Utils.getBitsFromBuffer(credit, 9, 12),
                    mCreditId = Utils.getBitsFromBuffer(credit, 56, 12),
                    mCredit = Utils.getBitsFromBufferSigned(credit, 77, 16) xor 0x7fff.inv(),
                    // byte 0-2.5: unknown const
                    mType = Utils.getBitsFromBuffer(card[0, 2].data, 20, 4),
                    mTrips = getTrips(card),
                    mSubscriptions = getSubscriptions(card, index))
        }

        private fun getTrips(card: ClassicCard): List<TransactionTrip> {
            val transactions = (0..27).mapNotNull { transactionId ->
                OVChipTransaction.parseClassic(card[35 + transactionId / 7]
                        .readBlocks(transactionId % 7 * 2, 2))
            }.groupingBy { it.id }.reduce { _, transaction, nextTransaction ->
                if (transaction.isTapOff)
                // check for two consecutive (duplicate) logouts, skip the second one
                    transaction
                else
                // handle two consecutive (duplicate) logins, skip the first one
                    nextTransaction
            }.values.toMutableList()

            return TransactionTrip.merge(transactions)
        }

        fun getSubscriptions(card: ClassicCard, index: OVChipIndex): List<OVChipSubscription> {
            val data = card[39].readBlocks(if (index.recentSubscriptionSlot) 3 else 1, 2)

            /*
         * TODO / FIXME
         * The card can store 15 subscriptions and stores pointers to some extra information
         * regarding these subscriptions. The problem is, it only stores 12 of these pointers.
         * In the code used here we get the subscriptions according to these pointers,
         * but this means that we could miss a few subscriptions.
         *
         * We could get the last few by looking at what has already been collected and get the
         * rest ourself, but they will lack the extra information because it simply isn't
         * there.
         *
         * Or rewrite this and just get all the subscriptions and discard the ones that are
         * invalid. Afterwards we can get the extra information if it's available.
         *
         * For more info see:
         * Dutch:   http://ov-chipkaart.pc-active.nl/Indexen
         * English: http://ov-chipkaart.pc-active.nl/Indexes
         */
            val count = Utils.getBitsFromBuffer(data, 0, 4)
            return (0 until count).map {
                val bits = Utils.getBitsFromBuffer(data, 4 + it * 21, 21)

                /* Based on info from ovc-tools by ocsr ( https://github.com/ocsrunl/ ) */
                val type1 = Utils.getBitsFromInteger(bits, 13, 8)
                //val type2 = Utils.getBitsFromInteger(bits, 7, 6)
                val used = Utils.getBitsFromInteger(bits, 6, 1)
                //val rest = Utils.getBitsFromInteger(bits, 4, 2)
                val subscriptionIndexId = Utils.getBitsFromInteger(bits, 0, 4)
                val subscriptionAddress = index.subscriptionIndex[subscriptionIndexId - 1]
                val subData = card[32 + subscriptionAddress / 5].readBlocks(subscriptionAddress % 5 * 3, 3)

                OVChipSubscription.parse(subData, type1, used)
            }.sortedWith(Comparator { s1, s2 -> (s1.id ?: 0).compareTo(s2.id ?: 0) })
        }

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val sector = sectors[0]

                if (sector is UnauthorizedClassicSector || sector is InvalidClassicSector)
                    return false

                // Starting at 0×010, 8400 0000 0603 a000 13ae e401 xxxx 0e80 80e8 seems to exist on all OVC's (with xxxx different).
                // http://www.ov-chipkaart.de/back-up/3-8-11/www.ov-chipkaart.me/blog/index7e09.html?page_id=132
                val blockData = sector.readBlocks(1, 1)
                return Arrays.equals(blockData.copyOfRange(0, 11), OVC_HEADER)
            }

            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME, null)

            override fun parseTransitData(classicCard: ClassicCard) = parse(classicCard)

            override fun earlySectors() = 1

            override fun getAllCards() = listOf(CARD_INFO)

            override fun check(classicCard: ClassicCard) = classicCard.sectors.size == 40 && earlyCheck(classicCard.sectors)
        }

        fun convertDate(date: Int): Calendar? = En1545FixedInteger.parseDate(date, OvcLookup.TIME_ZONE)
    }
}
