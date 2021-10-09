/*
 * NewShenzhenTrip.kt
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

package au.id.micolous.metrodroid.transit.china

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class NewShenzhenTrip (override val capsule: ChinaTripCapsule): ChinaTripAbstract() {
    override val endStation: Station?
        get() = when (transport) {
            SZT_METRO -> StationTableReader.getStation(SHENZHEN_STR,
                    (mStation and 0xffffff00).toInt(),
                    (mStation shr 8).toString(16)).addAttribute(
                    Localizer.localizeString(R.string.szt_station_gate,
                            (mStation and 0xff).toString(16)))
            else -> null
        }

    override val mode: Mode
        get() {
            if (isTopup)
                return Mode.TICKET_MACHINE
            return when (transport) {
                SZT_METRO -> Mode.METRO
                SZT_BUS -> Mode.BUS
                else -> Mode.OTHER
            }
        }

    override val routeName: FormattedString?
        get() = when (transport) {
                SZT_BUS -> StationTableReader.getLineName(SHENZHEN_STR, mStation.toInt(),
                        "" + mStation)
                else -> null
            }

    override val humanReadableRouteID: String?
        get() = when (transport) {
                SZT_BUS -> NumberUtils.intToHex(mStation.toInt())
                else -> null
            }

    override val startTimestamp: Timestamp?
        get() = if (transport == SZT_METRO) null else timestamp

    override val endTimestamp: Timestamp?
        get() = if (transport != SZT_METRO) null else timestamp

    constructor(data: ImmutableByteArray) : this(ChinaTripCapsule(data))

    override fun getAgencyName(isShort: Boolean) = when (transport) {
            SZT_METRO -> Localizer.localizeFormatted(R.string.szt_metro)
            SZT_BUS -> Localizer.localizeFormatted(R.string.szt_bus)
            else -> Localizer.localizeFormatted(R.string.unknown_format, transport)
        }

    companion object {
        private const val SZT_BUS = 3
        private const val SZT_METRO = 6
        private const val SHENZHEN_STR = "shenzhen"
    }
}
