/*
 * ClipperData.java
 *
 * Copyright 2011 "an anonymous contributor"
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2011 Chris Hundt <hundt@google.com>
 * Copyright 2011 David Hoover <karma@deadmoose.com>
 * Copyright 2011 Devin Carraway <git@devin.com>
 * Copyright 2012 Jason Hsu <jashsu@gmail.com>
 * Copyright 2012 Sebastian Oliva <tian2992@gmail.com>
 * Copyright 2012 Shayan Guha <shayan@coliloquy.com>
 * Copyright 2013 Mike Castleman <m@mlcastle.net>
 * Copyright 2014 Bao-Long Nguyen-Trong <baolong@inkling.com>
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
package au.id.micolous.metrodroid.transit.clipper;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.util.ImmutableMapBuilder;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Map;

final class ClipperData {
    static final int AGENCY_ACTRAN = 0x01;
    static final int AGENCY_BART = 0x04;
    static final int AGENCY_CALTRAIN = 0x06;
    static final int AGENCY_CCTA = 0x08;
    static final int AGENCY_GGT = 0x0b;
    static final int AGENCY_SAMTRANS = 0x0f;
    static final int AGENCY_VTA = 0x11;
    static final int AGENCY_MUNI = 0x12;
    static final int AGENCY_GG_FERRY = 0x19;
    static final int AGENCY_SF_BAY_FERRY = 0x1b;
    // Following codes are for refills
    static final int AGENCY_CALTRAIN_8RIDE = 0x173; 
    static final int AGENCY_WHOLE_FOODS = 0x2cf;

    static final Map<Integer, String> AGENCIES = new ImmutableMapBuilder<Integer, String>()
            .put(AGENCY_ACTRAN, "Alameda-Contra Costa Transit District")
            .put(AGENCY_BART, "Bay Area Rapid Transit")
            .put(AGENCY_CALTRAIN, "Caltrain")
            .put(AGENCY_CCTA, "Contra Costa Transportation Authority")
            .put(AGENCY_GGT, "Golden Gate Transit")
            .put(AGENCY_SAMTRANS, "San Mateo County Transit District")
            .put(AGENCY_VTA, "Santa Clara Valley Transportation Authority")
            .put(AGENCY_MUNI, "San Francisco Municipal")
            .put(AGENCY_GG_FERRY, "Golden Gate Ferry")
            .put(AGENCY_SF_BAY_FERRY, "San Francisco Bay Ferry")
            .put(AGENCY_CALTRAIN_8RIDE, "Caltrain 8-Rides")
            .put(AGENCY_WHOLE_FOODS, "Whole Foods")
            .build();

    static final Map<Integer, String> SHORT_AGENCIES = new ImmutableMapBuilder<Integer, String>()
            .put(AGENCY_ACTRAN, "ACTransit")
            .put(AGENCY_BART, "BART")
            .put(AGENCY_CALTRAIN, "Caltrain")
            .put(AGENCY_CCTA, "CCTA")
            .put(AGENCY_GGT, "GGT")
            .put(AGENCY_SAMTRANS, "SAMTRANS")
            .put(AGENCY_VTA, "VTA")
            .put(AGENCY_MUNI, "Muni")
            .put(AGENCY_GG_FERRY, "GG Ferry")
            .put(AGENCY_SF_BAY_FERRY, "SF Bay Ferry")
            .put(AGENCY_CALTRAIN_8RIDE, "Caltrain")
            .put(AGENCY_WHOLE_FOODS, "Whole Foods")
            .build();

    static final Map<Integer, String> GG_FERRY_ROUTES = new ImmutableMapBuilder<Integer, String>()
            .put(0x03, "Larkspur")
            .put(0x04, "San Francisco")
            .build();

    private static final String CLIPPER_STR = "clipper";

    private ClipperData() {
    }

    public static Station getStation(int agency, int stationId) {
        Station s = StationTableReader.getStationNoFallback(CLIPPER_STR,(agency << 16) | stationId);
        if (s != null)
            return s;
        if (agency == ClipperData.AGENCY_MUNI) {
            return null; // Coach number is not collected
        }

        if (agency == ClipperData.AGENCY_GGT || agency == ClipperData.AGENCY_CALTRAIN) {
            if (stationId == 0xffff)
                return Station.nameOnly(Utils.localizeString(R.string.clipper_end_of_line));
            return Station.nameOnly(Utils.localizeString(R.string.clipper_zone_number, "0x" + Long.toString(stationId, 16)));
        }

        return Station.unknown("0x" + Integer.toHexString(agency) + "/0x" + Long.toString(stationId, 16));
    }
}
