/*
 * StationTableReader.java
 * Reader for Metrodroid Station Table (MdST) files.
 *
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
package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Trip

actual class StationTableReader {
    actual companion object {
        actual fun getStationNoFallback(reader: String?, id: Int, humanReadableId: String): Station? = null

        actual fun getStation(reader: String?, id: Int, humanReadableId: String): Station = Station.unknown(humanReadableId)

        private fun fallbackName(humanReadableId: String): String = Localizer.localizeString(R.string.unknown_format, humanReadableId)

        actual fun getOperatorDefaultMode(reader: String?, id: Int): Trip.Mode = Trip.Mode.OTHER

        actual fun getLineName(reader: String?, id: Int, humanReadableId: String): String? = fallbackName(humanReadableId)

        actual fun getLineMode(reader: String?, id: Int): Trip.Mode? = null

        actual fun getOperatorName(reader: String?, id: Int, isShort: Boolean, humanReadableId: String): String? = fallbackName(humanReadableId)

        actual fun getNotice(reader: String?): String? = null
    }
}
