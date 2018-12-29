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

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize

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

    override fun getBalance() = if (mBalance == null) TransitCurrency.RUB(estimatedBalance) else TransitCurrency.RUB(mBalance)

    override fun getSerialNumber() = formatSerial(mSerial)

    override fun getCardName() = nameCard(mCardType)

    override fun getInfo(): List<ListItem>? {
        val regionNum = mCardType shr 16
        val cardInfo = CARDS[mCardType]
        val regionRsrcIdx = cardInfo?.locationId
        val regionName = (
                if (regionRsrcIdx != null)
                    Utils.localizeString(regionRsrcIdx)
                else
                    (REGIONS[regionNum]?.first ?: Integer.toHexString(regionNum))
                )
        return listOf(
                ListItem(R.string.zolotaya_korona_region, regionName),
                ListItem(R.string.card_type, cardInfo?.name ?: mCardType.toString(16)),
                // Printed in hex on the receipt
                ListItem(R.string.card_serial_number, mCardSerial.toUpperCase()),
                ListItem(R.string.refill_counter, mRefill?.mCounter?.toString() ?: "0"))
    }

    override fun getTrips() = listOfNotNull(mTrip) + listOfNotNull(mRefill)

    companion object {
        private val REGIONS = hashMapOf(
                // List of cities is taken from Zolotaya Korona website. Regions match
                // license plate regions
                //
                // Probably doesn't make sense to i18n as
                // the name is only used as fallback if the card is not known
                // Gorno-Altaysk
                0x04 to Pair("Altai Republic", "Asia/Krasnoyarsk"),
                // Syktyvkar and Ukhta
                0x11 to Pair("Komi Republic", "Europe/Kirov"),
                // Biysk
                0x22 to Pair("Altai Krai", "Asia/Krasnoyarsk"),
                // Krasnodar and Sochi
                0x23 to Pair("Krasnodar Krai", "Europe/Moscow"),
                // Vladivostok
                0x25 to Pair("Primorsky Krai", "Asia/Vladivostok"),
                // Khabarovsk
                0x27 to Pair("Khabarovsk Krai", "Asia/Vladivostok"),
                // Blagoveshchensk
                0x28 to Pair("Amur Oblast", "Asia/Yakutsk"),
                // Arkhangelsk
                0x29 to Pair("Arkhangelsk Oblast", "Europe/Moscow"),
                // Petropavlovsk-Kamchatsky
                0x41 to Pair("Kamchatka Krai", "Asia/Kamchatka"),
                // Kemerovo and Novokuznetsk
                0x42 to Pair("Kemerovo Oblast", "Asia/Novokuznetsk"),
                // Kurgan
                0x45 to Pair("Kurgan Oblast", "Asia/Yekaterinburg"),
                // Veliky Novgorod
                0x53 to Pair("Novgorod Oblast", "Europe/Moscow"),
                // Novosibirsk
                0x54 to Pair("Novosibirsk Oblast", "Asia/Novosibirsk"),
                // Omsk
                0x55 to Pair("Omsk Oblast", "Asia/Omsk"),
                // Orenburg
                0x56 to Pair("Orenburg Oblast", "Asia/Yekaterinburg"),
                // Pskov
                0x60 to Pair("Pskov Oblast", "Europe/Moscow"),
                // Samara
                0x63 to Pair("Samara Oblast", "Europe/Samara"),
                // Kholmsk
                0x65 to Pair("Sakhalin Oblast", "Asia/Sakhalin"),
                0x74 to Pair("Chelyabinsk Oblast", "Asia/Yekaterinburg"),
                // Yaroslavl
                0x76 to Pair("Yaroslavl Oblast", "Europe/Moscow"),
                // Birobidzhan
                0x79 to Pair("Jewish Autonomous Oblast", "Asia/Vladivostok")
        )

        private val CARDS = hashMapOf(
                0x760500 to CardInfo.Builder()
                        .setName(Utils.localizeString(R.string.card_name_yaroslavl_etk))
                        .setLocation(R.string.location_yaroslavl)
                        .setCardType(CardType.MifareClassic)
                        .setKeysRequired()
                        .setPreview()
                        .build(),
                0x230100 to CardInfo.Builder()
                        .setName(Utils.localizeString(R.string.card_name_krasnodar_etk))
                        .setLocation(R.string.location_krasnodar)
                        .setCardType(CardType.MifareClassic)
                        .setKeysRequired()
                        .setPreview()
                        .build()
        )

        private fun nameCard(type: Int) = CARDS[type]?.name
                ?: (Utils.localizeString(R.string.card_name_zolotaya_korona)
                        + " " + type.toString(16))

        private val FALLBACK_CARD_INFO = CardInfo.Builder()
                .setName(Utils.localizeString(R.string.card_name_zolotaya_korona))
                .setLocation(R.string.location_russia)
                .setCardType(CardType.MifareClassic)
                .setKeysRequired()
                .setPreview()
                .build()

        fun parseTime(time: Int, cardType: Int): Calendar? {
            if (time == 0)
                return null
            val tz = TimeZone.getTimeZone(REGIONS[cardType shr 16]?.second ?: "Europe/Moscow")
            val g = GregorianCalendar(tz)
            val pseudoUnixTime = time * 1000L
            g.timeInMillis = pseudoUnixTime
            // Not entirely correct around DST change but Russia doesn't
            // do it anymore
            g.add(Calendar.MILLISECOND, -tz.getOffset(pseudoUnixTime))
            return g
        }

        private fun getSerial(card: ClassicCard) = Utils.getHexString(card[15, 2].data,
                4, 10).substring(0, 19)

        private fun getCardType(card: ClassicCard) = Utils.byteArrayToInt(card[15, 1].data,
                10, 3)

        private fun formatSerial(serial: String) = Utils.groupString(serial, " ", 4, 5, 5)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun getAllCards() = listOf(FALLBACK_CARD_INFO) + CARDS.values

            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
                    nameCard(getCardType(card)),
                    formatSerial(getSerial(card)))

            override fun parseTransitData(classicCard: ClassicCard): TransitData {
                val cardType = getCardType(classicCard)

                val balance = if (classicCard[6] is UnauthorizedClassicSector) null else
                    Utils.byteArrayToIntReversed(classicCard[6, 0].data, 0, 4)

                val refill = ZolotayaKoronaRefill.parse(classicCard[4, 1].data, cardType)
                val trip = ZolotayaKoronaTrip.parse(classicCard[4, 2].data, cardType, refill, balance)

                return ZolotayaKoronaTransitData(
                        mSerial = getSerial(classicCard),
                        mCardSerial = Utils.getHexString(classicCard[0, 0].data, 0, 4),
                        mCardType = cardType,
                        mBalance = balance,
                        mTrip = trip,
                        mRefill = refill
                )
            }

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val toc = sectors[0][1].data
                // Check toc entries for sectors 10,12,13,14 and 15
                return Utils.byteArrayToInt(toc, 8, 2) == 0x18ee
                        && Utils.byteArrayToInt(toc, 12, 2) == 0x18ee
            }

            override fun earlySectors() = 1
        }
    }
}
