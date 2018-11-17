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

import android.support.annotation.StringRes
import java.util.ArrayList
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.UnauthorizedException
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

    override fun getCardInfo(): CardInfo = cardInfo(mCardType)

    override fun getInfo(): List<ListItem>? {
        val li = ArrayList<ListItem>()
        val regionNum = mCardType shr 16
        val cardInfo = CARDS[mCardType]
        val regionRsrcIdx = cardInfo?.locationId
        val regionName = (
                if (regionRsrcIdx != null)
                    Utils.localizeString(regionRsrcIdx)
                else
                    (REGIONS[regionNum]?.first ?: Integer.toHexString(regionNum))
                )
        li.add(ListItem(R.string.zolotaya_korona_region, regionName))
        li.add(ListItem(R.string.card_type, cardInfo?.name ?: mCardType.toString(16)))
        // Printed in hex on the receipt
        li.add(ListItem(R.string.card_serial_number, mCardSerial.toUpperCase()))
        li.add(ListItem(R.string.refill_counter, mRefill?.mCounter?.toString() ?: "0"))
        return li
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
                // Iaroslavl
                0x76 to Pair("Iaroslavl Oblast", "Europe/Moscow"),
                // Birobidzhan
                0x79 to Pair("Jewish Autonomous Oblast", "Asia/Vladivostok")
        )

        private val CARDS = hashMapOf(
                0x760500 to CardInfo.Builder()
                        .setName(R.string.card_name_iaroslavl_etk)
                        .setLocation(R.string.location_iaroslavl)
                        .setCardType(CardType.MifareClassic)
                        .setKeysRequired()
                        .setPreview()
                        .build(),
                0x230100 to CardInfo.Builder()
                        .setName(R.string.card_name_krasnodar_etk)
                        .setLocation(R.string.location_krasnodar)
                        .setCardType(CardType.MifareClassic)
                        .setKeysRequired()
                        .setPreview()
                        .build()
        )

        private fun cardInfo(type: Int) = CARDS[type] ?: FALLBACK_CARD_INFO

        @StringRes
        private fun nameCard(type: Int) = CARDS[type]?.nameId ?: 0

        private fun placeholderNameCard(type: Int) =
                (Utils.localizeString(R.string.card_name_zolotaya_korona) + " " + type.toString(16))

        private val FALLBACK_CARD_INFO = CardInfo.Builder()
                .setName(R.string.card_name_zolotaya_korona)
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

        private fun getSerial(card: ClassicCard) = Utils.getHexString(card.getSector(15)
                .getBlock(2).data, 4, 10).substring(0, 19)

        private fun getCardType(card: ClassicCard) = Utils.byteArrayToInt(card.getSector(15)
                .getBlock(1).data, 10, 3)

        private fun formatSerial(serial: String) = Utils.groupString(serial, " ", 4, 5, 5)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory() {
            override fun getAllCards() = listOf(FALLBACK_CARD_INFO) + CARDS.values

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
                val type = getCardType(card)
                val name = nameCard(type)
                val serial = formatSerial(getSerial(card))

                return if (name != 0) {
                    TransitIdentity(name, serial)
                } else {
                    TransitIdentity(placeholderNameCard(type), serial)
                }
            }

            override fun parseTransitData(classicCard: ClassicCard): TransitData {
                val cardType = getCardType(classicCard)

                val sector4 = classicCard.getSector(4)
                val sector6 = classicCard.getSector(6)
                val balance = if (sector6 is UnauthorizedClassicSector) null else
                    Utils.byteArrayToIntReversed(sector6.getBlock(0).data, 0, 4)

                val refill = ZolotayaKoronaRefill.parse(sector4.getBlock(1).data, cardType)
                val trip = ZolotayaKoronaTrip.parse(sector4.getBlock(2).data, cardType, refill, balance)

                return ZolotayaKoronaTransitData(
                        mSerial = getSerial(classicCard),
                        mCardSerial = Utils.getHexString(classicCard.getSector(0).getBlock(0).data, 0, 4),
                        mCardType = cardType,
                        mBalance = balance,
                        mTrip = trip,
                        mRefill = refill
                )
            }

            override fun check(card: ClassicCard) = try {
                check(card.getSector(0))
            } catch (ignored: IndexOutOfBoundsException) {
                // If that sector number is too high, then it's not for us.
                // If we can't read we can't do anything
                false
            } catch (ignored: UnauthorizedException) {
                false
            }

            private fun check(sector0: ClassicSector): Boolean {
                try {
                    val toc = sector0.getBlock(1).data
                    // Check toc entries for sectors 10,12,13,14 and 15
                    return Utils.byteArrayToInt(toc, 8, 2) == 0x18ee && Utils.byteArrayToInt(toc, 12, 2) == 0x18ee
                } catch (ignored: IndexOutOfBoundsException) {
                    // If that sector number is too high, then it's not for us.
                    // If we can't read we can't do anything
                } catch (ignored: UnauthorizedException) {
                }

                return false
            }

            override fun earlySectors() = 1

            // Determining exact card requires last sector, so just put it as Zolotaya Korona
            override fun earlyCardInfo(sectors: List<ClassicSector>) = if (check(sectors[0])) FALLBACK_CARD_INFO else null
        }
    }
}
