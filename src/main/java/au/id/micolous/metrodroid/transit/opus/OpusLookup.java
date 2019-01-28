/*
 * OpusLookup.java
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

package au.id.micolous.metrodroid.transit.opus;

import android.support.annotation.Nullable;
import android.util.SparseIntArray;

import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

public class OpusLookup extends En1545LookupSTR {
    private static final String OPUS_STR = "opus";
    private static final TimeZone TZ = TimeZone.getTimeZone("America/Montreal");

    // For opus we ignore transport
    @Override
    public String getRouteName(Integer routeNumber, Integer routeVariant, Integer agency, Integer transport) {
        if (routeNumber == null || routeNumber == 0)
            return null;
        if (agency == null)
            agency = 0;
        return StationTableReader.Companion.getLineName(OPUS_STR, routeNumber | (agency << 16));
    }

    // Opus doesn't store stations
    @Override
    public Station getStation(int station, Integer agency, Integer transport) {
        return null;
    }

    private static final SparseIntArray SUBSCRIPTIONS = new SparseIntArray();

    static {
        SUBSCRIPTIONS.put(0xb1, R.string.monthly_subscription);
        SUBSCRIPTIONS.put(0xb2, R.string.weekly_subscription);
        SUBSCRIPTIONS.put(0x1c7, R.string.single_trips);
    }

    @Override
    public String getSubscriptionName(Integer agency, Integer contractTariff) {
        if (contractTariff == null)
            return null;
        int resId = SUBSCRIPTIONS.get(contractTariff, 0);
        if (resId != 0)
            return Localizer.INSTANCE.localizeString(resId);
        return Localizer.INSTANCE.localizeString(R.string.unknown_format, contractTariff);
    }

    @Override
    public TransitCurrency parseCurrency(int price) {
        return TransitCurrency.CAD(price);
    }

    @Override
    public TimeZone getTimeZone() {
        return TZ;
    }

    private final static OpusLookup sInstance = new OpusLookup();

    private OpusLookup() { super(OPUS_STR);}

    public static En1545Lookup getInstance() {
        return sInstance;
    }
}
