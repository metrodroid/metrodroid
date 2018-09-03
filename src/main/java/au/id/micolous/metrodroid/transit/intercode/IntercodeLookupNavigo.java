/*
 * IntercodeLookupNavigo.java
 *
 * Copyright 2009-2013 by 'L1L1'
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

import java.util.HashMap;
import java.util.Map;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

class IntercodeLookupNavigo extends IntercodeLookupSTR {
    private static final Map<Integer, String> SECTOR_NAMES = new HashMap<>();
    private static final String NAVIGO_STR = "navigo";

    static {
        // TODO: Move this to MdSt
        SECTOR_NAMES.put(1, "Cité");
        SECTOR_NAMES.put(2, "Rennes");
        SECTOR_NAMES.put(3, "Villette");
        SECTOR_NAMES.put(4, "Montparnasse");
        SECTOR_NAMES.put(5, "Nation");
        SECTOR_NAMES.put(6, "Saint-Lazare");
        SECTOR_NAMES.put(7, "Auteuil");
        SECTOR_NAMES.put(8, "République");
        SECTOR_NAMES.put(9, "Austerlitz");
        SECTOR_NAMES.put(10, "Invalides");
        SECTOR_NAMES.put(11, "Sentier");
        SECTOR_NAMES.put(12, "Île Saint-Louis");
        SECTOR_NAMES.put(13, "Daumesnil");
        SECTOR_NAMES.put(14, "Italie");
        SECTOR_NAMES.put(15, "Denfert");
        SECTOR_NAMES.put(16, "Félix Faure");
        SECTOR_NAMES.put(17, "Passy");
        SECTOR_NAMES.put(18, "Étoile");
        SECTOR_NAMES.put(19, "Clichy - Saint Ouen");
        SECTOR_NAMES.put(20, "Montmartre");
        SECTOR_NAMES.put(21, "Lafayette");
        SECTOR_NAMES.put(22, "Buttes Chaumont");
        SECTOR_NAMES.put(23, "Belleville");
        SECTOR_NAMES.put(24, "Père Lachaise");
        SECTOR_NAMES.put(25, "Charenton");
        SECTOR_NAMES.put(26, "Ivry - Villejuif");
        SECTOR_NAMES.put(27, "Vanves");
        SECTOR_NAMES.put(28, "Issy");
        SECTOR_NAMES.put(29, "Levallois");
        SECTOR_NAMES.put(30, "Péreire");
        SECTOR_NAMES.put(31, "Pigalle");
    }

    IntercodeLookupNavigo() {
        super(NAVIGO_STR);
    }

    private static final int RATP = 3;
    private static final int SNCF = 2;

    @Override
    public Station getStation(int locationId, int agency, int transport) {
        if (locationId == 0)
            return null;
        int mdstStationId = locationId | (agency << 16) | (transport << 24);
        int sector_id = locationId >> 9;
        int station_id = (locationId >> 4) & 0x1F;
        String humanReadableId = Integer.toString(locationId);
        String fallBackName = Integer.toString(locationId);
        if (transport == IntercodeTrip.TRANSPORT_TRAIN && (agency == RATP || agency == SNCF)) {
            mdstStationId = (mdstStationId & 0xff00fff0) | 0x30000;
        }
        if ((agency == RATP || agency == SNCF)  && (transport == IntercodeTrip.TRANSPORT_METRO || transport== IntercodeTrip.TRANSPORT_TRAM)) {
             mdstStationId = (mdstStationId & 0x0000fff0) | 0x3020000;
             // TODO: i18n
             if (SECTOR_NAMES.containsKey(sector_id))
                 fallBackName = "sector " + SECTOR_NAMES.get(sector_id) + " station " + station_id;
             else
                 fallBackName = "sector " + sector_id + " station " + station_id;
             humanReadableId = sector_id + "/" + station_id;
        }

        Station st = StationTableReader.getStationNoFallback(NAVIGO_STR, mdstStationId, humanReadableId);
        if (st != null)
            return st;
        return Station.unknown(fallBackName);
    }

    @Override
    public String getSubscriptionName(Integer contractTariff) {
        if (contractTariff == null)
            return null;
        switch (contractTariff) {
            case 0:
                // TODO: i18n
                return "Forfait";
        }
        return Utils.localizeString(R.string.unknown_format, contractTariff);
    }
}
