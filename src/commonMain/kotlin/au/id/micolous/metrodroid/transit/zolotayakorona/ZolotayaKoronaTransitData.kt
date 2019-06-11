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
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
data class ZolotayaKoronaTransitData internal constructor(
        private val mSerial: String,
        private val mBalance: Int?,
        private val mCardSerial: String,
        private val mTrip: ZolotayaKoronaTrip?,
        private val mRefill: ZolotayaKoronaRefill?,
        private val mCardType: Int) : TransitData() {
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
                    (REGIONS[regionNum]?.first ?: regionNum.toString(16))
                )
        return listOf(
                ListItem(R.string.zolotaya_korona_region, regionName),
                ListItem(R.string.card_type, cardInfo?.name ?: mCardType.toString(16)),
                // Printed in hex on the receipt
                ListItem(R.string.card_serial_number, mCardSerial.toUpperCase()),
                ListItem(R.string.refill_counter, mRefill?.mCounter?.toString() ?: "0"))
    }

    override val trips get() = listOfNotNull(mTrip) + listOfNotNull(mRefill)

    companion object {
        private val REGIONS = mapOf(
                // List of cities is taken from Zolotaya Korona website. Regions match
                // license plate regions
                //
                // Probably doesn't make sense to i18n as
                // the name is only used as fallback if the card is not known
                // Gorno-Altaysk
                0x04 to Pair("Altai Republic", MetroTimeZone.KRASNOYARSK),
                // Syktyvkar and Ukhta
                0x11 to Pair("Komi Republic", MetroTimeZone.KIROV),
                // Biysk
                0x22 to Pair("Altai Krai", MetroTimeZone.KRASNOYARSK),
                // Krasnodar and Sochi
                0x23 to Pair("Krasnodar Krai", MetroTimeZone.MOSCOW),
                // Vladivostok
                0x25 to Pair("Primorsky Krai", MetroTimeZone.VLADIVOSTOK),
                // Khabarovsk
                0x27 to Pair("Khabarovsk Krai", MetroTimeZone.VLADIVOSTOK),
                // Blagoveshchensk
                0x28 to Pair("Amur Oblast", MetroTimeZone.YAKUTSK),
                // Arkhangelsk
                0x29 to Pair("Arkhangelsk Oblast", MetroTimeZone.MOSCOW),
                // Petropavlovsk-Kamchatsky
                0x41 to Pair("Kamchatka Krai", MetroTimeZone.KAMCHATKA),
                // Kemerovo and Novokuznetsk
                0x42 to Pair("Kemerovo Oblast", MetroTimeZone.NOVOKUZNETSK),
                // Kurgan
                0x45 to Pair("Kurgan Oblast", MetroTimeZone.YEKATERINBURG),
                // Veliky Novgorod
                0x53 to Pair("Novgorod Oblast", MetroTimeZone.MOSCOW),
                // Novosibirsk
                0x54 to Pair("Novosibirsk Oblast", MetroTimeZone.NOVOSIBIRSK),
                // Omsk
                0x55 to Pair("Omsk Oblast", MetroTimeZone.OMSK),
                // Orenburg
                0x56 to Pair("Orenburg Oblast", MetroTimeZone.YEKATERINBURG),
                // Pskov
                0x60 to Pair("Pskov Oblast", MetroTimeZone.MOSCOW),
                // Samara
                0x63 to Pair("Samara Oblast", MetroTimeZone.SAMARA),
                // Kholmsk
                0x65 to Pair("Sakhalin Oblast", MetroTimeZone.SAKHALIN),
                0x74 to Pair("Chelyabinsk Oblast", MetroTimeZone.YEKATERINBURG),
                // Yaroslavl
                0x76 to Pair("Yaroslavl Oblast", MetroTimeZone.MOSCOW),
                // Birobidzhan
                0x79 to Pair("Jewish Autonomous Oblast", MetroTimeZone.VLADIVOSTOK)
        )

        private val CARDS = mapOf(
                0x230100 to CardInfo(
                        name = Localizer.localizeString(R.string.card_name_krasnodar_etk),
                        locationId = R.string.location_krasnodar,
                        imageId = R.drawable.krasnodar_etk,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        preview = true),
                0x631500 to CardInfo(
                        name = Localizer.localizeString(R.string.card_name_samara_school),
                        locationId = R.string.location_samara,
                        imageId = R.drawable.samara_school,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        preview = true),
                0x632600 to CardInfo(
                        name = Localizer.localizeString(R.string.card_name_samara_etk),
                        locationId = R.string.location_samara,
                        imageId = R.drawable.samara_etk,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        preview = true),
                0x632700 to CardInfo(
                        name = Localizer.localizeString(R.string.card_name_samara_student),
                        locationId = R.string.location_samara,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        preview = true),
                0x760500 to CardInfo(
                        name = Localizer.localizeString(R.string.card_name_yaroslavl_etk),
                        locationId = R.string.location_yaroslavl,
                        imageId = R.drawable.yaroslavl_etk,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        preview = true)
        )

        private fun nameCard(type: Int) = CARDS[type]?.name
                ?: (Localizer.localizeString(R.string.card_name_zolotaya_korona)
                        + " " + type.toString(16))

        private val FALLBACK_CARD_INFO = CardInfo(
                name = Localizer.localizeString(R.string.card_name_zolotaya_korona),
                locationId = R.string.location_russia,
                cardType = CardType.MifareClassic,
                keysRequired = true,
                preview = true)

        fun parseTime(time: Int, cardType: Int): Timestamp? {
            if (time == 0)
                return null
            val tz = REGIONS[cardType shr 16]?.second ?: MetroTimeZone.MOSCOW
            val epoch = Epoch.local(1970, tz)
            // This is pseudo unix time with local day alwayscoerced to 86400 seconds
            return epoch.daySecond(time / 86400, time % 86400)
        }

        private fun getSerial(card: ClassicCard) = card[15, 2].data.getHexString(
                4, 10).substring(0, 19)

        private fun getCardType(card: ClassicCard) = card[15, 1].data.byteArrayToInt(
                10, 3)

        private fun formatSerial(serial: String) = NumberUtils.groupString(serial, " ", 4, 5, 5)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override val allCards get() = listOf(FALLBACK_CARD_INFO) + CARDS.values

            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
                    nameCard(getCardType(card)),
                    formatSerial(getSerial(card)))

            override fun parseTransitData(card: ClassicCard): TransitData {
                val cardType = getCardType(card)

                val balance = if (card[6] is UnauthorizedClassicSector) null else
                    card[6, 0].data.byteArrayToIntReversed(0, 4)

                val refill = ZolotayaKoronaRefill.parse(card[4, 1].data, cardType)
                val trip = ZolotayaKoronaTrip.parse(card[4, 2].data, cardType, refill, balance)

                return ZolotayaKoronaTransitData(
                        mSerial = getSerial(card),
                        mCardSerial = card[0, 0].data.getHexString(0, 4),
                        mCardType = cardType,
                        mBalance = balance,
                        mTrip = trip,
                        mRefill = refill
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
