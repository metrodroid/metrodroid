/*
 * IntercodeLookupSTR.java
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

package au.id.micolous.metrodroid.transit.intercode;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

public class IntercodeLookupSTR implements IntercodeLookup {
    private final String mStr;

    IntercodeLookupSTR(String str) {
        mStr = str;
    }

    @Override
    public String getRouteName(Integer routeNumber, Integer routeVariant, int agency, int transport) {
        if (routeNumber == null)
            return null;
        int routeId = (routeNumber) | (agency << 16) | (transport << 24);
        String routeReadable = Integer.toString(routeNumber);
        if (routeVariant != null) {
            routeReadable += "/" + routeVariant;
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
    public Station getStation(int station, int agency, int transport) {
        if (station == 0)
            return null;
        return StationTableReader.getStation(
                mStr,
                station | (agency << 16) | (transport << 24),
                "0x" + Integer.toHexString(station));

    }

    @Override
    public String getSubscriptionName(Integer contractTariff) {
        if (contractTariff == null)
            return null;
        return Utils.localizeString(R.string.unknown_format, contractTariff);
    }
}
