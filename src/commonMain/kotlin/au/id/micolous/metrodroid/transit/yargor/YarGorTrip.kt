/*
 * YarGorTrip.kt
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

package au.id.micolous.metrodroid.transit.yargor

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.hexString

@Parcelize
data class YarGorTrip(
        override val startTimestamp: TimestampFull,
        private val mA: Int,
        private val mRoute: Int,
        private val mVehicle: Int,
        private val mB: Int,
        // sequential number of round trips that bus makes
        private val mTrackNumber: Int) : Trip() {
    override val fare: TransitCurrency?
        get() = null
    override val mode: Mode
        get() = StationTableReader.getLineMode(YARGOR_STR, mRoute)
                ?: when(mRoute/100) {
                    0,20 -> Mode.BUS
                    1 -> Mode.TRAM
                    2,3 -> Mode.TROLLEYBUS
                    else -> Mode.OTHER
                }

    override fun getAgencyName(isShort: Boolean): FormattedString =
        when (mode) {
            Mode.TRAM -> Localizer.localizeFormatted(R.string.mode_tram)
            Mode.TROLLEYBUS -> Localizer.localizeFormatted(R.string.mode_trolleybus)
            Mode.BUS -> Localizer.localizeFormatted(R.string.mode_bus)
            else -> Localizer.localizeFormatted(R.string.unknown_format, mRoute / 100)
        }

    override val routeName: FormattedString
        get() = StationTableReader.getLineNameNoFallback(YARGOR_STR, mRoute) ?: FormattedString((mRoute % 100).toString())

    override val vehicleID: String?
        get() = mVehicle.toString()

    override fun getRawFields(level: TransitData.RawLevel): String? =
            "A=${mA.hexString}/B=${mB.hexString}" +
                    if (level == TransitData.RawLevel.ALL)
                        "/trackNumber=$mTrackNumber/route=$mRoute"
                    else ""

    companion object {
        private const val YARGOR_STR = "yargor"
        private fun parseTimestampBCD(data: ImmutableByteArray, off: Int): TimestampFull =
                TimestampFull(tz = YarGorTransitData.TZ,
                        year = 2000 + NumberUtils.convertBCDtoInteger(data[off].toInt() and 0xff),
                        month = NumberUtils.convertBCDtoInteger(data[off + 1].toInt() and 0xff) - 1,
                        day = NumberUtils.convertBCDtoInteger(data[off + 2].toInt() and 0xff),
                        hour = NumberUtils.convertBCDtoInteger(data[off + 3].toInt() and 0xff),
                        min = NumberUtils.convertBCDtoInteger(data[off + 4].toInt() and 0xff),
                        sec = NumberUtils.convertBCDtoInteger(data[off + 5].toInt() and 0xff))

        fun parse(input: ImmutableByteArray): YarGorTrip? {
            if (input[9] == 0.toByte())
                return null
            return YarGorTrip(startTimestamp = parseTimestampBCD(input, 9),
                    mVehicle = input.byteArrayToIntReversed(0, 2),
                    mA = input[2].toInt() and 0xff,
                    mRoute = input.byteArrayToIntReversed(3, 2),
                    mB = input.byteArrayToIntReversed(5, 2),
                    mTrackNumber = input.byteArrayToInt(7, 2))
        }
    }
}
