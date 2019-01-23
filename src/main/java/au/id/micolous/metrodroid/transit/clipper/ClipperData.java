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

import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.NumberUtils;
import org.jetbrains.annotations.NonNls;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.ImmutableMapBuilder;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Map;

final class ClipperData {
    static final int AGENCY_CALTRAIN = 0x06;
    static final int AGENCY_GGT = 0x0b;
    static final int AGENCY_MUNI = 0x12;
    static final int AGENCY_GG_FERRY = 0x19;
    static final int AGENCY_BAY_FERRY = 0x1b;

    static final Map<Integer, String> GG_FERRY_ROUTES = new ImmutableMapBuilder<Integer, String>()
            .put(0x03, "Larkspur")
            .put(0x04, "San Francisco")
            .build();

    static final String CLIPPER_STR = "clipper";

    private ClipperData() {
    }

    public static Trip.Mode getMode(int agency) {
        return StationTableReader.getOperatorDefaultMode(CLIPPER_STR, agency);
    }

    public static String getAgencyName(int agency, boolean isShort) {
        return StationTableReader.getOperatorName(CLIPPER_STR, agency, isShort);
    }


    public static Station getStation(int agency, int stationId, boolean isEnd) {
        @NonNls String humanReadableId = NumberUtils.INSTANCE.intToHex(agency) + "/" + NumberUtils.INSTANCE.intToHex(stationId);
        Station s = StationTableReader.getStationNoFallback(CLIPPER_STR,(agency << 16) | stationId,
                humanReadableId);
        if (s != null)
            return s;

        if (agency == ClipperData.AGENCY_GGT || agency == ClipperData.AGENCY_CALTRAIN) {
            if (stationId == 0xffff)
                return Station.nameOnly(Localizer.INSTANCE.localizeString(R.string.clipper_end_of_line));
            return Station.nameOnly(Localizer.INSTANCE.localizeString(R.string.clipper_zone_number, Integer.toString(stationId)));
        }

        // Placeholders
        if (stationId == (isEnd ? 0xffff : 0))
            return null;

        return Station.unknown(humanReadableId);
    }
}
