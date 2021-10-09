/*
 * ZolotayaKoronaTransitData.kt
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

package au.id.micolous.metrodroid.transit.zolotayakorona

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
data class ZolotayaKoronaTransitData internal constructor(
        private val mSerial: String,
        private val mBalance: Int?,
        private val mCardSerial: String,
        private val mTrip: ZolotayaKoronaTrip?,
        private val mRefill: ZolotayaKoronaRefill?,
        private val mCardType: Int,
        private val mStatus: Int,
        private val mDiscountCode: Int,
        private val mSequenceCtr: Int,
        private val mTail: ImmutableByteArray) : TransitData() {
    private val estimatedBalance: Int
        get() {
            // a trip followed by refill. Assume only one refill.
            if (mRefill != null && mTrip != null && mRefill.mTime > mTrip.mTime)
                return mTrip.estimatedBalance + mRefill.mAmount
            // Last transaction was a trip
            if (mTrip != null)
                return mTrip.estimatedBalance
            // No trips. Look for refill
            if (mRefill != null)
                return mRefill.mAmount
            // Card was never used or refilled
            return 0
        }

    override val balance get() = if (mBalance == null) TransitCurrency.RUB(estimatedBalance) else TransitCurrency.RUB(mBalance)

    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = nameCard(mCardType)

    override val info get(): List<ListItem>? {
        val regionNum = mCardType shr 16
        val cardInfo = CARDS[mCardType]
        val regionRsrcIdx = cardInfo?.locationId
        val regionName = (
                if (regionRsrcIdx != null)
                    Localizer.localizeString(regionRsrcIdx)
                else
                    RussiaTaxCodes.BCDToName(regionNum)
                )
        return listOf(
                ListItem(R.string.zolotaya_korona_region, regionName),
                ListItem(R.string.card_type, cardInfo?.name ?: mCardType.toString(16)),
                ListItem(R.string.zolotaya_korona_discount, discountMap[mDiscountCode]?.let {
                    Localizer.localizeString(it) } ?: Localizer.localizeString(R.string.unknown_format, mDiscountCode.toString(16))),
                // Printed in hex on the receipt
                ListItem(R.string.card_serial_number, mCardSerial.uppercase()),
                ListItem(R.string.refill_counter, mRefill?.mCounter?.toString() ?: "0"))
    }

    override fun getRawFields(level: RawLevel): List<ListItem>? = listOf(
            // Unsure about next 2 fields, hence they are hidden in raw fields
            ListItem("Status", mStatus.toString()),
            ListItem("Issue seqno", mSequenceCtr.toString()),
            ListItem(FormattedString("ID-Tail"), mTail.toHexDump())
    )

    override val trips get() = listOfNotNull(mTrip) + listOfNotNull(mRefill)

    companion object {
        private val discountMap = mapOf(
                0x46 to R.string.zolotaya_korona_discount_111,
                0x47 to R.string.zolotaya_korona_discount_100,
                0x48 to R.string.zolotaya_korona_discount_200
        )
        private val INFO_CARDS = mapOf(
                0x230100 to CardInfo(
                        name = R.string.card_name_krasnodar_etk,
                        locationId = R.string.location_krasnodar,
                        imageId = R.drawable.krasnodar_etk,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        region = TransitRegion.RUSSIA,
                        keysRequired = true, keyBundle = "zolotayakoronakrasnodar",
                        preview = true),
                0x560200 to CardInfo(
                        name = R.string.card_name_orenburg_ekg,
                        locationId = R.string.location_orenburg,
                        imageId = R.drawable.orenburg_ekg,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        region = TransitRegion.RUSSIA,
                        keysRequired = true, keyBundle = "zolotayakoronaorenburg",
                        preview = true),
                0x632600 to CardInfo(
                         name = R.string.card_name_samara_etk,
                         locationId = R.string.location_samara,
                         imageId = R.drawable.samara_etk,
                         imageAlphaId = R.drawable.iso7810_id1_alpha,
                         cardType = CardType.MifareClassic,
                         region = TransitRegion.RUSSIA,
                         keysRequired = true, keyBundle = "zolotayakoronasamara",
                         preview = true),
                0x760500 to CardInfo(
                        name = R.string.card_name_yaroslavl_etk,
                        locationId = R.string.location_yaroslavl,
                        imageId = R.drawable.yaroslavl_etk,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        region = TransitRegion.RUSSIA,
                        keysRequired = true, keyBundle = "zolotayakoronayaroslavl",
                        preview = true)
        )
        private val EXTRA_CARDS = mapOf(
                0x562300 to CardInfo(
                        name = R.string.card_name_orenburg_school,
                        locationId = R.string.location_orenburg,
                        imageId = R.drawable.orenburg_ekg,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        region = TransitRegion.RUSSIA,
                        keysRequired = true, keyBundle = "zolotayakoronaorenburg",
                        preview = true),
                0x562400 to CardInfo(
                        name = R.string.card_name_orenburg_student,
                        locationId = R.string.location_orenburg,
                        imageId = R.drawable.orenburg_ekg,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        region = TransitRegion.RUSSIA,
                        keysRequired = true, keyBundle = "zolotayakoronaorenburg",
                        preview = true),
                0x631500 to CardInfo(
                        name = R.string.card_name_samara_school,
                        locationId = R.string.location_samara,
                        imageId = R.drawable.samara_etk,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        region = TransitRegion.RUSSIA,
                        keysRequired = true, keyBundle = "zolotayakoronasamara",
                        preview = true),
                0x632700 to CardInfo(
                        name = R.string.card_name_samara_student,
                        locationId = R.string.location_samara,
                        imageId = R.drawable.samara_etk,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        region = TransitRegion.RUSSIA,
                        keysRequired = true, keyBundle = "zolotayakoronasamara",
                        preview = true),
                0x633500 to CardInfo(
                        name = R.string.card_name_samara_garden_dacha,
                        locationId = R.string.location_samara,
                        imageId = R.drawable.samara_etk,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        region = TransitRegion.RUSSIA,
                        keysRequired = true, keyBundle = "zolotayakoronasamara",
                        preview = true)
        )
        private val CARDS = INFO_CARDS + EXTRA_CARDS

        private fun nameCard(type: Int) = CARDS[type]?.name
                ?: (Localizer.localizeString(R.string.card_name_zolotaya_korona)
                        + " " + type.toString(16))

        private val FALLBACK_CARD_INFO = CardInfo(
                name = Localizer.localizeString(R.string.card_name_zolotaya_korona),
                locationId = R.string.location_russia,
                imageId = R.drawable.zolotayakorona,
                cardType = CardType.MifareClassic,
                keysRequired = true,
                region = TransitRegion.RUSSIA,
                preview = true)

        fun parseTime(time: Int, cardType: Int): Timestamp? {
            if (time == 0)
                return null
            val tz = RussiaTaxCodes.BCDToTimeZone(cardType shr 16)
            val epoch = Epoch.local(1970, tz)
            // This is pseudo unix time with local day always coerced to 86400 seconds
            return epoch.daySecond(time / 86400, time % 86400)
        }

        private fun getSerial(card: ClassicCard) = card[15, 2].data.getHexString(
                4, 10).substring(0, 19)

        private fun getCardType(card: ClassicCard) = card[15, 1].data.byteArrayToInt(
                10, 3)

        private fun formatSerial(serial: String) = NumberUtils.groupString(serial, " ", 4, 5, 5)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override val allCards get() = listOf(FALLBACK_CARD_INFO) + INFO_CARDS.values

            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
                    nameCard(getCardType(card)),
                    formatSerial(getSerial(card)))

            override fun parseTransitData(card: ClassicCard): TransitData {
                val cardType = getCardType(card)

                val balance = if (card[6] is UnauthorizedClassicSector) null else
                    card[6, 0].data.byteArrayToIntReversed(0, 4)

                val infoBlock = card[4,0].data
                val refill = ZolotayaKoronaRefill.parse(card[4, 1].data, cardType)
                val trip = ZolotayaKoronaTrip.parse(card[4, 2].data, cardType, refill, balance)

                return ZolotayaKoronaTransitData(
                        mSerial = getSerial(card),
                        mCardSerial = card[0, 0].data.getHexString(0, 4),
                        mCardType = cardType,
                        mBalance = balance,
                        mTrip = trip,
                        mRefill = refill,
                        mStatus = infoBlock.getBitsFromBuffer(60, 4),
                        mSequenceCtr = infoBlock.byteArrayToInt(8, 2),
                        mDiscountCode = infoBlock[10].toInt() and 0xff,
                        mTail = infoBlock.sliceOffLen(11, 5)
                )
            }

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val toc = sectors[0][1].data
                // Check toc entries for sectors 10,12,13,14 and 15
                return toc.byteArrayToInt(8, 2) == 0x18ee
                        && toc.byteArrayToInt(12, 2) == 0x18ee
            }

            override val earlySectors get() = 1
        }
    }
}
