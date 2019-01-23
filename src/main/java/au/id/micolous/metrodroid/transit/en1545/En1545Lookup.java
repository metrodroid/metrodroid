/*
 * IntercodeLookup.java
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

import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.util.NumberUtils;
import org.jetbrains.annotations.NonNls;

import java.util.TimeZone;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public interface En1545Lookup {
    String getRouteName(Integer routeNumber, Integer routeVariant, Integer agency, Integer transport);

    @NonNls
    @Nullable
    default String getHumanReadableRouteId(@Nullable Integer routeNumber,
                                           @Nullable Integer routeVariant,
                                           @Nullable Integer agency,
                                           @Nullable Integer transport) {
        if (routeNumber == null)
            return null;
        @NonNls String routeReadable = NumberUtils.INSTANCE.intToHex(routeNumber);
        if (routeVariant != null) {
            routeReadable += "/" + NumberUtils.INSTANCE.intToHex(routeVariant);
        }
        return routeReadable;
    }

    String getAgencyName(Integer agency, boolean isShort);

    Station getStation(int station, Integer agency, Integer transport);

    @Nullable
    String getSubscriptionName(Integer agency, Integer contractTariff);

    TransitCurrency parseCurrency(int price);

    TimeZone getTimeZone();

    // Only relevant if EventCode is not present.
    Trip.Mode getMode(Integer agency, Integer route);
}
