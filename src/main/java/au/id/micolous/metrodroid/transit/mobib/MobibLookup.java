/*
 * MobibLookup.java
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

package au.id.micolous.metrodroid.transit.mobib;

import java.util.TimeZone;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR;
import au.id.micolous.metrodroid.util.StationTableReader;

public class MobibLookup extends En1545LookupSTR {
    static final String MOBIB_STR = "mobib";

    private static final int BUS = 0xf;

    @Override
    public String getRouteName(Integer routeNumber, Integer routeVariant, Integer agency, Integer transport) {
        if (routeNumber == null)
            return null;
        if (agency == null)
            return null;
        if (agency == BUS) {
            return Integer.toString(routeNumber);
        }
        return null;
    }

    @Override
    public Station getStation(int station, Integer agency, Integer transport) {
        if (station == 0)
            return null;
        if (agency == null)
            agency = 0;
        return StationTableReader.getStation(MOBIB_STR, station | (agency << 22));
    }

    @Override
    public String getSubscriptionName(Integer agency, Integer contractTariff) {
        if (contractTariff == null)
            return null;
        return Integer.toString(contractTariff);
    }

    @Override
    public TransitCurrency parseCurrency(int price) {
        return TransitCurrency.EUR(price);
    }

    @Override
    public TimeZone getTimeZone() {
        return MobibTransitData.TZ;
    }

    private final static MobibLookup sInstance = new MobibLookup();

    private MobibLookup() {super(MOBIB_STR);}

    public static En1545Lookup getInstance() {
        return sInstance;
    }
}
