/*
 * LisboaVivaLookup.java
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

package au.id.micolous.metrodroid.transit.lisboaviva;

import android.support.annotation.Nullable;

import java.util.TimeZone;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR;
import au.id.micolous.metrodroid.util.StationTableReader;

class LisboaVivaLookup extends En1545LookupSTR {
    private static final String LISBOA_VIVA_STR = "lisboa_viva";
    private static final TimeZone TZ = TimeZone.getTimeZone("Europe/Lisbon");

    private final static LisboaVivaLookup sInstance = new LisboaVivaLookup();
    public static final int ZAPPING_TARIFF = 33592;
    public static final int ZAPPING_AGENCY = 31;
    public static final int AGENCY_CP = 3;
    public static final int ROUTE_CASCAIS_SADO = 40960;

    private LisboaVivaLookup() {super(LISBOA_VIVA_STR);}

    public static En1545Lookup getInstance() {
        return sInstance;
    }

    @Override
    public String getRouteName(Integer routeNumber, Integer routeVariant, Integer agency, Integer transport) {
        if (routeNumber == null || routeNumber == 0)
            return null;

        if (agency == null)
            return Integer.toString(routeNumber);
        if (agency == 1)
            return Integer.toString(routeNumber);
        routeNumber = mungeRouteNumber(agency, routeNumber);
        return StationTableReader.Companion.getLineName(LISBOA_VIVA_STR, (agency << 16) | routeNumber,
                Integer.toString(routeNumber));
    }

    @Nullable
    @Override
    public String getHumanReadableRouteId(@Nullable Integer routeNumber,
                                          @Nullable Integer routeVariant,
                                          @Nullable Integer agency,
                                          @Nullable Integer transport) {
        if (routeNumber == null || agency == null) {
            // Null route number = unknown route
            // Null agency = return raw route number (so no need to duplicate)
            return null;
        }

        return Integer.toString(mungeRouteNumber(agency, routeNumber));
    }

    @Override
    public Station getStation(int station, Integer agency, Integer routeNumber) {
        if (station == 0 || agency == null || routeNumber == null)
            return null;
        routeNumber = mungeRouteNumber(agency, routeNumber);
        if (agency == 2)
            station = station >> 2;
        return StationTableReader.Companion.getStation(LISBOA_VIVA_STR, station | (routeNumber << 8) | (agency << 24), Integer.toString(station));
    }

    private int mungeRouteNumber(int agency, int routeNumber) {
        if (agency == 16)
            return routeNumber & 0xf;
        if (agency == AGENCY_CP && routeNumber != ROUTE_CASCAIS_SADO)
            return 4096;
        return routeNumber;
    }

    @Override
    public String getSubscriptionName(Integer agency, Integer contractTariff) {
        if (contractTariff == null || agency == null)
            return null;

        if (agency == 15) {
            switch (contractTariff) {
                case 73:
                    return  "Ass. PAL - LIS";
                case 193:
                    return  "Ass. FOG - LIS";
                case 217:
                    return  "Ass. PRA - LIS";
            }
        }
        if (agency == 16 && contractTariff == 5)
	        return "Passe MTS";
        if (agency == 30) {
            switch (contractTariff) {
                case 113:
                    return "Metro / RL 12";
                case 316:
                    return "Vermelho A1";
                case 454:
                    return "Metro/CP - R. Mouro/Meleças";
                case 720:
                    return "Navegante urbano";
                case 725:
                    return "Navegante rede";
                case 733:
                    return "Navegante SL TCB Barreiro";
                case 1088:
                    return "Fertagus PAL - LIS + ML";
            }
        }
        if (agency == ZAPPING_AGENCY && contractTariff == ZAPPING_TARIFF)
            return "Zapping";
        return Integer.toString(contractTariff);
    }

    @Override
    public TransitCurrency parseCurrency(int price) {
        return TransitCurrency.EUR(price);
    }

    @Override
    public TimeZone getTimeZone() {
        return TZ;
    }
}
