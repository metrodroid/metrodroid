/*
 * RavKavLookup.java
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

package au.id.micolous.metrodroid.transit.ravkav;

import java.util.TimeZone;

import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR;

class RavKavLookup extends En1545LookupSTR {
    private static final int EGGED = 0x3;
    private static final String RAVKAV_STR = "ravkav";
    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Jerusalem");

    private final static RavKavLookup sInstance = new RavKavLookup();

    private RavKavLookup() {super(RAVKAV_STR);}

    public static En1545Lookup getInstance() {
        return sInstance;
    }

    @Override
    public String getRouteName(Integer routeNumber, Integer routeVariant, Integer agency, Integer transport) {
        if (routeNumber == null || routeNumber == 0)
            return null;
        if (agency != null && agency == EGGED)
            return Integer.toString(routeNumber%1000);
        return Integer.toString(routeNumber);
    }

    @Override
    public String getSubscriptionName(Integer agency, Integer contractTariff) {
        if (contractTariff == null)
            return null;
        // TODO: Figure names out
        return Integer.toString(contractTariff);
    }

    @Override
    public TransitCurrency parseCurrency(int price) {
        return new TransitCurrency(price, "ILS");
    }

    @Override
    public TimeZone getTimeZone() {
        return TZ;
    }

    // Irrelevant as RavKAv has EventCode
    @Override
    public Trip.Mode getMode(Integer agency, Integer route) {
        return Trip.Mode.OTHER;
    }
}
