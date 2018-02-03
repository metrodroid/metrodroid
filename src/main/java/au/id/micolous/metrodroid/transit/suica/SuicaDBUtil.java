/*
 * SuicaDBUtil.java
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
package au.id.micolous.metrodroid.transit.suica;

import android.util.Log;

import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.util.StationTableReader;

/**
 * Helpers for accessing Suica-related stop databases.
 */

final class SuicaDBUtil {
    private static final String TAG = "SuicaUtil";

    /**
     * Gets bus stop information from the IruCa (イルカ) table.
     *
     * @param lineCode    Bus line ID (line code)
     * @param stationCode Bus stop ID (station code)
     * @return If the stop is known, a Station is returned describing it. If the stop is unknown,
     *         or there was some other database error, null is returned.
     */
    static Station getBusStop(int regionCode, int lineCode, int stationCode) {
        lineCode &= 0xFF;
        stationCode &= 0xFF;

        int stationId = (lineCode << 8) + stationCode;
        if (stationId == 0) return null;

        StationTableReader str = MetrodroidApplication.getInstance().getSuicaBusSTR();
        if (str == null) return null;

        try {
            return str.getStationById(stationId);
        } catch (Exception e) {
            Log.d(TAG, "error in getBusStop", e);
            return null;
        }
    }

    /**
     * Gets train station information from the Japan Rail (JR) table.
     *
     * @param regionCode  Train area/region ID (region code)
     * @param lineCode    Train line ID (line code)
     * @param stationCode Train station ID (station code)
     * @return If the stop is known, a Station is returned describing it. If the stop is unknown,
     *         or there was some other database error, null is returned.
     */
    static Station getRailStation(int regionCode, int lineCode, int stationCode) {
        int areaCode = (regionCode >> 6) & 0xFF;
        lineCode &= 0xFF;
        stationCode &= 0xFF;

        int stationId = (areaCode << 16) + (lineCode << 8) + stationCode;
        if (stationId == 0) return null;

        StationTableReader str = MetrodroidApplication.getInstance().getSuicaRailSTR();
        if (str == null) return null;

        try {
            return str.getStationById(stationId);
        } catch (Exception e) {
            Log.d(TAG, "error in getRailStation", e);
            return null;
        }
    }
}
