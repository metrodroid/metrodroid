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

import au.id.micolous.metrodroid.util.NumberUtils;
import org.jetbrains.annotations.NonNls;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

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
        @NonNls String routeReadable = getHumanReadableRouteId(routeNumber, routeVariant, agency, transport);
        return StationTableReader.Companion.getLineName(mStr, routeId, routeReadable);
    }

    @Override
    public String getAgencyName(Integer agency, boolean isShort) {
        if (agency == null || agency == 0)
            return null;
        return StationTableReader.Companion.getOperatorName(mStr, agency, isShort);
    }

    @Override
    public Station getStation(int station, Integer agency, Integer transport) {
        if (station == 0)
            return null;
        if (agency == null)
            agency = 0;
        if (transport == null)
            transport = 0;
        return StationTableReader.Companion.getStation(
                mStr,
                station | (agency << 16) | (transport << 24),
                NumberUtils.INSTANCE.intToHex(station));

    }

    @Override
    public Trip.Mode getMode(Integer agency, Integer route) {
        Trip.Mode mode = null;

        if (route != null) {
            if (agency == null)
                mode = StationTableReader.Companion.getLineMode(mStr, route);
            else
                mode = StationTableReader.Companion.getLineMode(mStr, route | (agency << 16));
        }
        if (mode != null)
            return mode;
        if (agency == null)
            return Trip.Mode.OTHER;
        mode = StationTableReader.Companion.getOperatorDefaultMode(mStr, agency);
        if (mode != null)
            return mode;
        return Trip.Mode.OTHER;
    }
}
