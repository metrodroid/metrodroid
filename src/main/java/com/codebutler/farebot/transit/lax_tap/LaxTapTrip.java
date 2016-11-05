/*
 * SeqGoTrip.java
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

/**
 * Represents trip events on LAX TAP card.
 */
public class LaxTapTrip extends NextfareTrip {

    @Override
    public String getAgencyName() {
        String agency = LaxTapData.AGENCIES.get(mModeInt, null);
        if (agency == null) {
            return Utils.localizeString(R.string.unknown_format, mModeInt);
        }

        return agency;
    }

    @Override
    public String getStartStationName() {
        if (mStartStation == 0 || mModeInt == LaxTapData.AGENCY_SANTA_MONICA) {
            return null;
        } else {
            Station s = getStartStation();
            if (s == null) {
                return Utils.localizeString(R.string.unknown_format, mStartStation);
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public Station getStartStation() {
        return LaxTapDBUtil.getStation(mStartStation, mModeInt);
    }

    @Override
    public String getEndStationName() {
        if (mEndStation == 0 || mModeInt == LaxTapData.AGENCY_SANTA_MONICA) {
            return null;
        } else {
            Station s = getEndStation();
            if (s == null) {
                return Utils.localizeString(R.string.unknown_format, mEndStation);
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public Station getEndStation() {
        return LaxTapDBUtil.getStation(mEndStation, mModeInt);
    }

    @Override
    public Mode getMode() {
        return mMode;
    }

    public LaxTapTrip() {}

    public LaxTapTrip(Parcel in) {
        super(in);
    }

    public static final Creator<LaxTapTrip> CREATOR = new Creator<LaxTapTrip>() {

        public LaxTapTrip createFromParcel(Parcel in) {
            return new LaxTapTrip(in);
        }

        public LaxTapTrip[] newArray(int size) {
            return new LaxTapTrip[size];
        }
    };
}
