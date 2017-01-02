/*
 * LaxTapTrip.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package com.codebutler.farebot.transit.lax_tap;

import android.os.Parcel;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.nextfare.NextfareTrip;
import com.codebutler.farebot.util.Utils;

import au.id.micolous.farebot.R;

import static com.codebutler.farebot.transit.lax_tap.LaxTapData.AGENCY_METRO;
import static com.codebutler.farebot.transit.lax_tap.LaxTapData.METRO_BUS_ROUTES;
import static com.codebutler.farebot.transit.lax_tap.LaxTapData.METRO_BUS_START;

/**
 * Represents trip events on LAX TAP card.
 */
public class LaxTapTrip extends NextfareTrip {

    public static final Creator<LaxTapTrip> CREATOR = new Creator<LaxTapTrip>() {

        public LaxTapTrip createFromParcel(Parcel in) {
            return new LaxTapTrip(in);
        }

        public LaxTapTrip[] newArray(int size) {
            return new LaxTapTrip[size];
        }
    };

    public LaxTapTrip() {
    }

    public LaxTapTrip(Parcel in) {
        super(in);
    }

    @Override
    public String getAgencyName() {
        String agency = LaxTapData.AGENCIES.get(mModeInt, null);
        if (agency == null) {
            return Utils.localizeString(R.string.unknown_format, mModeInt);
        }

        return agency;
    }

    @Override
    public String getRouteName() {
        if (mModeInt == AGENCY_METRO && mStartStation >= METRO_BUS_START) {
            // Metro Bus uses the station_id for route numbers.
            return METRO_BUS_ROUTES.get(mStartStation, Utils.localizeString(R.string.unknown_format, mStartStation));

        }

        // Normally not possible to guess what the route is.
        return null;
    }

    private String getStationName(int stationId) {
        if (stationId == 0 || mModeInt == LaxTapData.AGENCY_SANTA_MONICA) {
            return null;
        } else if (mModeInt == AGENCY_METRO && stationId >= METRO_BUS_START) {
            // We don't know the station.
            return null;
        }

        // Fall back to database
        Station s = getStation(stationId);
        if (s == null) {
            return Utils.localizeString(R.string.unknown_format, stationId);
        } else {
            return s.getStationName();
        }
    }

    private Station getStation(int stationId) {
        if (stationId == 0 || mModeInt == LaxTapData.AGENCY_SANTA_MONICA) {
            // Santa Monica Bus doesn't use this.
            return null;
        }

        if (mModeInt == AGENCY_METRO && stationId >= METRO_BUS_START) {
            // Metro uses this for route names.
            return null;
        }

        return LaxTapDBUtil.getStation(stationId, mModeInt);
    }

    @Override
    public String getStartStationName() {
        return getStationName(mStartStation);
    }

    @Override
    public Station getStartStation() {
        return getStation(mStartStation);
    }

    @Override
    public String getEndStationName() {
        return getStationName(mEndStation);
    }

    @Override
    public Station getEndStation() {
        return getStation(mEndStation);
    }

    @Override
    public Mode getMode() {
        return mMode;
    }
}
