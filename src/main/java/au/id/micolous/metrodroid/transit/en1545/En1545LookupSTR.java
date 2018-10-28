/*
 * En1545LookupSTR.java
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

package au.id.micolous.metrodroid.transit.en1545;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;

public abstract class En1545LookupSTR implements En1545Lookup {
    private final String mStr;

    protected En1545LookupSTR(String str) {
        mStr = str;
    }

    @Override
    public String getRouteName(Integer routeNumber, Integer routeVariant, Integer agency, Integer transport) {
        if (routeNumber == null)
            return null;
        if (agency == null)
            agency = 0;
        if (transport == null)
            transport = 0;
        int routeId = (routeNumber) | (agency << 16) | (transport << 24);
        String routeReadable = "0x" + Integer.toHexString(routeNumber);
        if (routeVariant != null) {
            routeReadable += "/0x" + Integer.toHexString(routeVariant);
        }
        return StationTableReader.getLineName(mStr, routeId, routeReadable);
    }

    @Override
    public String getAgencyName(Integer agency, boolean isShort) {
        if (agency == null || agency == 0)
            return null;
        return StationTableReader.getOperatorName(mStr, agency, isShort);
    }

    @Override
    public Station getStation(int station, Integer agency, Integer transport) {
        if (station == 0)
            return null;
        if (agency == null)
            agency = 0;
        if (transport == null)
            transport = 0;
        return StationTableReader.getStation(
                mStr,
                station | (agency << 16) | (transport << 24),
                "0x" + Integer.toHexString(station));

    }

    @Override
    public Trip.Mode getMode(Integer agency, Integer route) {
        Trip.Mode mode = null;

        if (route != null) {
            if (agency == null)
                mode = StationTableReader.getLineMode(mStr, route);
            else
                mode = StationTableReader.getLineMode(mStr, route | (agency << 16));
        }
        if (mode != null)
            return mode;
        if (agency == null)
            return Trip.Mode.OTHER;
        mode = StationTableReader.getOperatorDefaultMode(mStr, agency);
        if (mode != null)
            return mode;
        return Trip.Mode.OTHER;
    }
}
