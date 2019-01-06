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

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.SparseIntArray;

import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR;
import au.id.micolous.metrodroid.util.Utils;

public class AdelaideLookup extends En1545LookupSTR {
    private static final TimeZone TZ = TimeZone.getTimeZone("Australia/Adelaide");
    private static final int AGENCY_ADL_METRO = 1;

    private static final SparseIntArray TARIFFS = new SparseIntArray();

    static {
        TARIFFS.put(0x804, R.string.adelaide_ticket_type_regular);
        TARIFFS.put(0x808, R.string.adelaide_ticket_type_concession);
        // TODO: handle other tickets

        // TODO: handle monthly subscriptions
    }


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

    @Nullable
    @Override
    public String getSubscriptionName(Integer agency, Integer contractTariff) {
        if (contractTariff == null)
            return null;
        @StringRes int tariff = TARIFFS.get(contractTariff, 0);
        if (tariff == 0) {
            return Utils.intToHex(contractTariff);
        } else {
            return Utils.localizeString(tariff);
        }
    }

    boolean isPurseTariff(@Nullable Integer agency, @Nullable Integer contractTariff) {
        if (agency == null || agency != AGENCY_ADL_METRO || contractTariff == null) {
            return false;
        }

        // TODO: Exclude monthly tickets when implemented
        return TARIFFS.indexOfKey(contractTariff) >= 0;
    }

    @Override
    public String getRouteName(Integer routeNumber, Integer routeVariant, Integer agency, Integer transport) {
        if (routeNumber == 0)
            return null;

        return super.getRouteName(routeNumber, routeVariant, agency, transport);
    }
}
