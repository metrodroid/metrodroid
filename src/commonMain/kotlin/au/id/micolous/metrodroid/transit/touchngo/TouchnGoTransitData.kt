/*
 * TouchnGoTransitData.kt
 *
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.transit.touchngo

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.StationTableReader

private const val NAME = "Touch 'n Go"
private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_malaysia,
        imageId = R.drawable.touchngo,
        cardType = CardType.MifareClassic,
        keysRequired = true, keyBundle = "touchngo")

private fun parseTimestamp(input: ImmutableByteArray, off: Int): TimestampFull {
    val hour = input.getBitsFromBuffer(off * 8, 5)
    val min = input.getBitsFromBuffer(off * 8 + 5, 6)
    val sec = input.getBitsFromBuffer(off * 8 + 11, 6)
    val year = input.getBitsFromBuffer(off * 8 + 17, 6) + 1990
    val month = input.getBitsFromBuffer(off * 8 + 23, 4)
    val day = input.getBitsFromBuffer(off * 8 + 27, 5)
    return TimestampFull(tz = MetroTimeZone.KUALA_LUMPUR, year = year, month = month - 1, day = day, hour = hour,
            min = min, sec = sec)
}

private fun parseDaystamp(input: ImmutableByteArray, off: Int): Daystamp {
    val y = input.getBitsFromBuffer(off * 8 + 1, 6) + 1990
    val month = input.getBitsFromBuffer(off * 8 + 7, 4)
    val d = input.getBitsFromBuffer(off * 8 + 11, 5)
    return Daystamp(year = y, month = month - 1, day = d)
}

private const val TNG_STR = "touchngo"

abstract private class TouchnGoTripCommon : Trip(), Parcelable {
    abstract val header: ImmutableByteArray
    protected val transactionId
        get() = header.byteArrayToInt(0, 2)
    protected val agencyRaw
        get() = header.sliceOffLen(2, 4)
    private val newBalance: Int
        get() = header.byteArrayToIntReversed(6, 4)
    private val amount: Int
        get() = header.byteArrayToInt(10, 2)
    protected val headerTimestamp
        get() = parseTimestamp(header, 12)
    override val startTimestamp: Timestamp?
        get() = headerTimestamp
    override val fare: TransitCurrency?
        get() = TransitCurrency(amount, "MYR")

    override fun getAgencyName(isShort: Boolean): String? {
        return StationTableReader.getOperatorName(
                TNG_STR,
                agencyRaw.byteArrayToInt(),
                isShort,
                if (agencyRaw.isASCII()) agencyRaw.readASCII() else agencyRaw.toHexString())
    }
}

@Parcelize
private data class TouchnGoRefill(
        override val header: ImmutableByteArray) : TouchnGoTripCommon() {
    override val fare: TransitCurrency?
        get() = super.fare?.negate()
    override val mode: Mode
        get() = Trip.Mode.TICKET_MACHINE

    companion object {
        fun parse (sec: ClassicSector): TouchnGoRefill? {
            if (sec[0].isEmpty)
                return null

            return TouchnGoRefill(
                    header = sec[0].data
            )
        }
    }
}

@Parcelize
private data class TouchnGoGeneric(
        override val header: ImmutableByteArray,
        override val routeName: String?,
        override val mode: Mode) : TouchnGoTripCommon() {

    companion object {
        fun parse (sec: ClassicSector,
                   mode: Mode, routeName: String? = null): TouchnGoGeneric? {
            if (sec[0].isEmpty && sec[1].isEmpty && sec[2].isEmpty)
                return null

            return TouchnGoGeneric(
                    mode = mode,
                    header = sec[0].data,
                    routeName = routeName
            )
        }
    }
}

@Parcelize
private data class TouchnGoStationId(
        private val station: Int,
        private val machine: Int
) : Parcelable {
    fun resolve(): Station = StationTableReader.getStation(TNG_STR, station,
            station.toString()).addAttribute(
            "machine $machine"
    )
    companion object {
        fun parse(raw: ImmutableByteArray, off: Int) =
                TouchnGoStationId (
                        raw.byteArrayToInt(off, 2),
                        raw.byteArrayToInt(off + 2, 2)
                )
    }
}

private fun isTripInProgress(sec: ClassicSector) = sec[1].data.byteArrayToInt(2, 4) != 0

@Parcelize
private data class TouchnGoTrip(
        override val header: ImmutableByteArray,
        private val startStationCode: TouchnGoStationId?,
        private val endStationCode: TouchnGoStationId) : TouchnGoTripCommon() {
    override val mode: Mode
        get() = StationTableReader.getOperatorDefaultMode(TNG_STR, agencyRaw.byteArrayToInt())
    override val startTimestamp: Timestamp?
        get() = null
    override val endTimestamp: Timestamp?
        get() = headerTimestamp
    override val endStation: Station?
        get() = endStationCode.resolve()
    override val startStation: Station?
        get() = startStationCode?.resolve()

    companion object {
        fun parse (sec: ClassicSector): TouchnGoTrip? {
            if (sec[0].isEmpty)
                return null

            return TouchnGoTrip(
                    header = sec[0].data,
                    startStationCode = if(isTripInProgress(sec)) null else TouchnGoStationId.parse(sec[1].data, 6),
                    endStationCode = TouchnGoStationId.parse(sec[2].data, 6)
            )
        }
    }
}

@Parcelize
private data class TouchnGoInProgressTrip(
        override val startTimestamp: TimestampFull,
        private val startStationCode: TouchnGoStationId,
        val agencyRawShort: ImmutableByteArray
): Trip() {
    override fun getAgencyName(isShort: Boolean): String? {
        return if (agencyRawShort.isASCII()) agencyRawShort.readASCII() else agencyRawShort.toHexString()
    }
    override val fare: TransitCurrency?
        get() = null
    override val mode: Mode
        get() = Mode.OTHER
    override val startStation: Station?
        get() = startStationCode.resolve()

    companion object {
        fun parse (sec: ClassicSector): TouchnGoInProgressTrip? {
            if (!isTripInProgress(sec))
                return null

            val blk = sec[1].data

            return TouchnGoInProgressTrip(
                    agencyRawShort = blk.sliceOffLen(0, 2),
                    startTimestamp = parseTimestamp(blk, 2),
                    startStationCode = TouchnGoStationId.parse(blk, 6)
            )
        }
    }
}

@Parcelize
private data class TouchnGoTransitData(private val mBalance: Int, private val mSerial: Long,
                               override val trips: List<Trip>,
                               private val mCardNo: Int,
                               private val mStoredLuhn: Int,
                               private val mIssueCounter: Int,
                               private val mIssueDate: Daystamp,
                               private val mExpiryDate: Daystamp) : TransitData() {
    override val serialNumber: String?
        get() = NumberUtils.zeroPad(mSerial, 10)
    override val cardName get() = NAME

    override val balance get() = TransitBalanceStored(
            TransitCurrency(mBalance, "MYR"),
            name = null,
            validFrom = mIssueDate,
            validTo = mExpiryDate
    )

    override val info: List<ListItem>?
        get() {
            val partialCardNo = "6014640" + NumberUtils.zeroPad(mCardNo, 10)
            val cardNo = partialCardNo + NumberUtils.calculateLuhn(partialCardNo)
            return listOf(
                    ListItem("CardNo", cardNo)
            )
        }
}

class TouchnGoTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            NAME, NumberUtils.zeroPad(card[0, 0].data.byteArrayToLongReversed(0, 4), 10))

    override fun parseTransitData(card: ClassicCard): TransitData {
        return TouchnGoTransitData(
                mBalance = card[2, 0].data.byteArrayToIntReversed(0, 4),
                mSerial = card[0, 0].data.byteArrayToLongReversed(0, 4),
                trips = listOfNotNull(
                        TouchnGoGeneric.parse(card[5], Trip.Mode.POS,
                                "Toll"), // FIXME: type POS is kinda wrong
                        TouchnGoTrip.parse(card[6]),
                        TouchnGoInProgressTrip.parse(card[6]),
                        TouchnGoRefill.parse(card[7]),
                        TouchnGoGeneric.parse(card[8], Trip.Mode.POS)),
                mCardNo = card[0,2].data.byteArrayToInt(7, 4),
                mStoredLuhn = card[0,2].data[11].toInt() and 0xff,
                mIssueCounter = card[1,0].data.byteArrayToInt(12, 2),
                mIssueDate = parseDaystamp(card[1,0].data, 14),
                mExpiryDate = parseDaystamp(card[0,2].data, 14))
    }

    override fun earlyCheck(sectors: List<ClassicSector>) =
            sectors[0][1].data == ImmutableByteArray.fromHex("000102030405060708090a0b0c0d0e0f")

    override val earlySectors get() = 1
}
