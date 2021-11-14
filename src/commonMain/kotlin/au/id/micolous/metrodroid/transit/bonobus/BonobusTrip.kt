/*
 * BonobusTrip.kt
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.transit.bonobus

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.hexString

@Parcelize
data class BonobusTrip internal constructor(
        private val mTimestamp: Long,
        private val mFare: Int,
        private val mMode: Int,
        private val mA: Int,
        private val mStation: Int,
        private val mT: Int,
        private val mLine: Int,
        private val mVehicleNumber: Int) : Trip() {
    override val fare get() = TransitCurrency.EUR(if (mMode == MODE_REFILL) -mFare else mFare)
    override val mode get() = when (mMode) {
        MODE_BUS -> Mode.BUS
        MODE_REFILL -> Mode.TICKET_MACHINE
        else -> Mode.BUS
    }
    override val startTimestamp get() = parseTimestamp(mTimestamp)
    override fun getRawFields(level: TransitData.RawLevel): String?
            = "A=${mA.hexString}" + (if (level == TransitData.RawLevel.ALL) "/station=$mStation mode=$mMode L${NumberUtils.zeroPad(mLine,4)} T${NumberUtils.zeroPad(mT,4)}" else "")
    override val vehicleID get() = if (mVehicleNumber == 0) null else NumberUtils.zeroPad(mVehicleNumber, 4)
    override val routeName get() = if (mMode == MODE_BUS) FormattedString((mLine - 10).toString()) else null
    override val startStation: Station?
        get() = if (mStation == 1 || mStation == 0) null else StationTableReader.getStation(BONOBUS_STR, mStation,
                humanReadableId = mStation.toString())
    override fun getAgencyName(isShort: Boolean) = if (mMode == MODE_BUS) FormattedString(BUS_AGENCY_NAME) else null

    companion object {
        fun parse(raw: ImmutableByteArray): BonobusTrip? {
            if (raw.isAllZero())
                return null
            return BonobusTrip(
                mTimestamp=raw.byteArrayToLong(0, 4),
                mFare=raw.byteArrayToInt(6, 2),
                mMode=raw.getBitsFromBuffer(32, 4),
                mA=raw.getBitsFromBuffer(36, 12),
                mStation=raw.byteArrayToInt(8, 2),
                mT=raw.byteArrayToInt(10, 2),
                mLine=raw.byteArrayToInt(12, 2),
                mVehicleNumber=raw.byteArrayToInt(14, 2))
        }
        fun parseTimestamp(input: Long) =
            TimestampFull(
                MetroTimeZone.MADRID,
                (input shr 25).toInt() + 2000,
                ((input shr 21).toInt() and 0xf) - 1,
                (input shr 16).toInt() and 0x1f,
                (input shr 11).toInt() and 0x1f,
                (input shr 5).toInt() and 0x3f,
                (input shl 1).toInt() and 0x3f
            )
        private const val MODE_BUS = 8
        private const val MODE_REFILL = 12
        const val BONOBUS_STR = "cadiz"
        const val BUS_AGENCY_NAME = "Tranvía"
    }
}
