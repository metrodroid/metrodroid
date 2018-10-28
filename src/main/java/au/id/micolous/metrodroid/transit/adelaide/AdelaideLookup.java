/*
 * AdelaideLookup.java
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
package au.id.micolous.metrodroid.transit.adelaide;

import java.util.TimeZone;

import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR;

public class AdelaideLookup extends En1545LookupSTR {
    private static final TimeZone TZ = TimeZone.getTimeZone("Australia/Adelaide");
    static final int AGENCY_ADL_METRO = 1;
    static final int CONTRACT_PURSE = 0x804;

    @Override
    public TransitCurrency parseCurrency(int price) {
        return TransitCurrency.AUD(price);
    }

    @Override
    public TimeZone getTimeZone() {
        return TZ;
    }

    private final static AdelaideLookup sInstance = new AdelaideLookup();

    private AdelaideLookup() { super("adelaide");}

    public static AdelaideLookup getInstance() {
        return sInstance;
    }

    @Override
    public String getSubscriptionName(Integer agency, Integer contractTariff) {
        if (contractTariff == null)
            return null;
        return Integer.toString(contractTariff);
    }
}
