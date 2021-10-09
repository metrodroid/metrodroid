/*
 * SuicaDBUtil.kt
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
package au.id.micolous.metrodroid.transit.suica

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.StationTableReader

/**
 * Helpers for accessing Suica-related stop databases.
 */

object SuicaDBUtil {
    private const val TAG = "SuicaUtil"
    private const val SUICA_BUS_STR = "suica_bus"
    private const val SUICA_RAIL_STR = "suica_rail"

    /**
     * Gets bus stop information from the IruCa (イルカ) table.
     *
     * @param lineCode    Bus line ID (line code)
     * @param stationCode Bus stop ID (station code)
     * @return If the stop is known, a Station is returned describing it. If the stop is unknown,
     * or there was some other database error, null is returned.
     */
    internal fun getBusStop(regionCode: Int, lineCode: Int, stationCode: Int): Station? {
        val lineCodeLow = lineCode and 0xFF
        val stationCodeLow = stationCode and 0xFF
        val stationId = (lineCodeLow shl 8) + stationCodeLow
        return if (stationId == 0) null else StationTableReader.getStation(SUICA_BUS_STR, stationId,
                Localizer.localizeString(R.string.suica_bus_area_line_stop,
                        NumberUtils.intToHex(regionCode),
                        NumberUtils.intToHex(lineCodeLow),
                        NumberUtils.intToHex(stationCodeLow)))

    }

    /**
     * Gets train station information from the Japan Rail (JR) table.
     *
     * @param regionCode  Train area/region ID (region code)
     * @param lineCode    Train line ID (line code)
     * @param stationCode Train station ID (station code)
     * @return If the stop is known, a Station is returned describing it. If the stop is unknown,
     * or there was some other database error, null is returned.
     */
    fun getRailStation(regionCode: Int, lineCode: Int, stationCode: Int): Station? {
        val lineCodeLow = lineCode and 0xFF
        val stationCodeLow = stationCode and 0xFF
        val areaCode = regionCode shr 6 and 0xFF
        val stationId = (areaCode shl 16) + (lineCodeLow shl 8) + stationCodeLow
        return if (stationId == 0) null else StationTableReader.getStation(SUICA_RAIL_STR, stationId,
                Localizer.localizeString(R.string.suica_area_line_station,
                        NumberUtils.intToHex(regionCode),
                        NumberUtils.intToHex(lineCodeLow),
                        NumberUtils.intToHex(stationCodeLow)))

    }
}
