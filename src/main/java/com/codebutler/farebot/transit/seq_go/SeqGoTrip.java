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
package com.codebutler.farebot.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.nextfare.NextfareTrip;
import com.codebutler.farebot.util.Utils;

import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;

/**
 * Represents trip events on Go Card.
 */
public class SeqGoTrip extends NextfareTrip {

    @Override
    public String getAgencyName() {
        switch (mMode) {
            case FERRY:
                return "Transdev Brisbane Ferries";
            case TRAIN:
                // Domestic Airport == 9
                if (mStartStation == 9 || mEndStation == 9) {
                    // TODO: Detect International Airport station.
                    return "Airtrain";
                } else {
                    return "Queensland Rail";
                }
            default:
                return "TransLink";
        }
    }

    @Override
    public String getStartStationName() {
        if (mStartStation == 0) {
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
        return SeqGoDBUtil.getStation(mStartStation);
    }

    @Override
    public String getEndStationName() {
        if (mEndStation == 0) {
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
        return SeqGoDBUtil.getStation(mEndStation);
    }

    @Override
    public Mode getMode() {
        return mMode;
    }

    /**
     * This constructor is used for unit tests outside of the package
     * @param startStation Starting station ID.
     * @param endStation Ending station ID.
     * @param startTime Start time of the journey.
     * @param endTime End time of the journey.
     * @param journeyId Journey ID.
     * @param continuation True if this is a continuation of a previous journey (transfer).
     */
    public SeqGoTrip(int startStation, int endStation, GregorianCalendar startTime, GregorianCalendar endTime, int journeyId, boolean continuation) {
        mStartStation = startStation;
        mEndStation = endStation;
        mStartTime = startTime;
        mEndTime = endTime;
        mJourneyId = journeyId;
        mContinuation = continuation;
    }

    public int getJourneyId() {
        return mJourneyId;
    }

    public SeqGoTrip() {}

    public SeqGoTrip(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<SeqGoTrip> CREATOR = new Parcelable.Creator<SeqGoTrip>() {

        public SeqGoTrip createFromParcel(Parcel in) {
            return new SeqGoTrip(in);
        }

        public SeqGoTrip[] newArray(int size) {
            return new SeqGoTrip[size];
        }
    };
}
