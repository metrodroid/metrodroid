/*
 * WarsawTransitData.java
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

package au.id.micolous.metrodroid.transit.warsaw

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.hexString

/**
 * Warsaw cards.
 */

private fun parseDateTime(raw: ImmutableByteArray, off: Int) = if (raw.getBitsFromBuffer(off, 26) == 0) null else TimestampFull(
        MetroTimeZone.WARSAW,
        raw.getBitsFromBuffer(off, 6) + 2000,
        raw.getBitsFromBuffer(off+6, 4) - 1,
        raw.getBitsFromBuffer(off+10, 5),
        raw.getBitsFromBuffer(off+15, 5),
        raw.getBitsFromBuffer(off+20, 6)
)

private fun parseDate(raw: ImmutableByteArray, off: Int) = if (raw.getBitsFromBuffer(off, 16) == 0) null else Daystamp(
        raw.getBitsFromBuffer(off, 7) + 2000,
        raw.getBitsFromBuffer(off+7, 4) - 1,
        raw.getBitsFromBuffer(off+11, 5)
)

@Parcelize
data class WarsawTrip(
        override val startTimestamp: TimestampFull,
        private val mTripType: Int //educated guess
): Trip() {
    override val fare: TransitCurrency?
        get() = null
    override val mode: Mode
        get() = Mode.OTHER
}

@Parcelize
data class WarsawSubscription(
        override val validTo: Daystamp,
        private val mTicketType: Int // educated guess
): Subscription() {
    override val subscriptionName: String?
        get() = when (mTicketType) {
            0xbf6 -> Localizer.localizeString(R.string.warsaw_90_days)
            else -> Localizer.localizeString(R.string.unknown_format, mTicketType.hexString)
        }
}

@Parcelize
data class WarsawSector(
        private val mTripTimestamp: TimestampFull?,
        private val mExpiryTimestamp: Daystamp?,
        private val mTicketType: Int, // educated guess
        private val mTripType: Int, //educated guess
        private val mCounter: Int
): Parcelable, Comparable<WarsawSector> {
    fun getRawFields(level: TransitData.RawLevel): List<ListItem>? = listOf(
            ListItem(FormattedString("Trip timestamp"), mTripTimestamp?.format()),
            ListItem(FormattedString("Expiry timestamp"), mExpiryTimestamp?.format()),
            ListItem("Trip type", mTripType.hexString),
            ListItem("Ticket type", mTicketType.hexString),
            ListItem("Counter", mCounter.hexString)
    )
    override operator fun compareTo(other: WarsawSector): Int = when {
        mTripTimestamp == null && other.mTripTimestamp == null -> 0
        mTripTimestamp == null -> -1
        other.mTripTimestamp == null -> + 1
        mTripTimestamp.timeInMillis.compareTo(other = other.mTripTimestamp.timeInMillis) != 0 ->
            mTripTimestamp.timeInMillis.compareTo(other = other.mTripTimestamp.timeInMillis)
        else -> -((mCounter - other.mCounter) and 0xff).compareTo(0x80)
    }
    val trip get() = if (mTripTimestamp == null) null else WarsawTrip(mTripTimestamp, mTripType)
    val subscription get() = if (mExpiryTimestamp == null) null else WarsawSubscription(mExpiryTimestamp, mTicketType)
    companion object {
        fun parse(sec: ClassicSector) = WarsawSector(
                mCounter = sec[0].data.byteArrayToInt(1, 1),
                mExpiryTimestamp = parseDate(sec[0].data, 16),
                mTicketType = sec[0].data.getBitsFromBuffer(32, 12),
                mTripType = sec[0].data.byteArrayToInt(9, 3),
                mTripTimestamp = parseDateTime(sec[0].data, 44)
        )
    }
}

@Parcelize
data class WarsawTransitData (private val mSerial: Pair<Int, Int>,
                              private val sectorA: WarsawSector,
                              private val sectorB: WarsawSector): TransitData() {
    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    override val trips: List<Trip>?
        get() = listOfNotNull(sectorA.trip, sectorB.trip)

    override val subscriptions: List<Subscription>?
        get() = listOfNotNull(maxOf(sectorA, sectorB).subscription)

    override fun getRawFields(level: RawLevel): List<ListItem>? = listOf(
            HeaderListItem("Sector 2")) + sectorA.getRawFields(level).orEmpty() +
            listOf(HeaderListItem("Sector 3")) + sectorB.getRawFields(level).orEmpty()

    companion object {
        private const val NAME = "Warszawska Karta Miejska"
        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_warsaw,
                cardType = CardType.MifareClassic,
                resourceExtraNote = R.string.card_note_card_number_only,
                keysRequired = true, keyBundle = "warsaw",
                imageId = R.drawable.warsaw_card,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                region = TransitRegion.POLAND,
                preview = true)

        private fun formatSerial(serial: Pair<Int, Int>) =
                NumberUtils.zeroPad(serial.first, 3) + " " +
                        NumberUtils.zeroPad(serial.second, 8)

        private fun getSerial(card: ClassicCard) = Pair(
                card[0, 0].data[3].toInt() and 0xff,
                card[0, 0].data.byteArrayToIntReversed(0, 3))

        fun parse(card: ClassicCard): WarsawTransitData {
            return WarsawTransitData(mSerial = getSerial(card),
                    sectorA = WarsawSector.parse(card[2]),
                    sectorB = WarsawSector.parse(card[3]))
        }

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun parseTransitIdentity(card: ClassicCard) =
                    TransitIdentity(NAME,
                        formatSerial(getSerial(card)))

            override fun parseTransitData(card: ClassicCard) =
                    parse(card)

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val toc = sectors[0][1].data
                // Check toc entries for sectors 1, 2 and 3
                return (toc.byteArrayToInt(2, 2) == 0x1320
                        && toc.byteArrayToInt(4, 2) == 0x1320
                        && toc.byteArrayToInt(6, 2) == 0x1320)
            }

            override val earlySectors get() = 1

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
