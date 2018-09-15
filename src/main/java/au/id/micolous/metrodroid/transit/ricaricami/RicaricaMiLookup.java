/*
 * RicaricaMiLookup.java
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

package au.id.micolous.metrodroid.transit.ricaricami;

import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR;
import au.id.micolous.metrodroid.util.Utils;

public class RicaricaMiLookup extends En1545LookupSTR {
    public static final TimeZone TZ = TimeZone.getTimeZone("Europe/Rome");
    public static final int TRANSPORT_METRO = 1;
    public static final int TRANSPORT_TROLLEYBUS = 2;
    public static final int TRANSPORT_TRAM = 4;
    public static final int TARIFF_URBAN = 624;
    public static final int TARIFF_URBAN_2X6 = 612;

    private RicaricaMiLookup() {
        super("ricaricami");
    }

    @Override
    public TransitCurrency parseCurrency(int price) {
        return TransitCurrency.EUR(price);
    }

    @Override
    public TimeZone getTimeZone() {
        return TZ;
    }

    private final static RicaricaMiLookup sInstance = new RicaricaMiLookup();

    public static En1545Lookup getInstance() {
        return sInstance;
    }

    @Override
    public String getSubscriptionName(Integer agency, Integer contractTariff) {
        if (contractTariff == null)
            return null;
        if (contractTariff == TARIFF_URBAN)
            return "Urban ticket";
        if (contractTariff == TARIFF_URBAN_2X6)
            return "Urban weekly 2x6 ticket";
        return Utils.localizeString(R.string.unknown_format, contractTariff);
    }

    @Override
    public String getRouteName(Integer routeNumber, Integer routeVariant, Integer agency, Integer transport) {
        if (transport == TRANSPORT_METRO) {
            if (routeNumber == 101)
                return "M1";
            if (routeNumber == 107)
                return "M5";
            if (routeNumber == 301)
                return "M3";
        }
        if (routeNumber == null)
            return null;
        String routeReadable = Integer.toString(routeNumber);
        if (routeVariant != null) {
            routeReadable += "/" + routeVariant;
        }
        return routeReadable;
    }
}
