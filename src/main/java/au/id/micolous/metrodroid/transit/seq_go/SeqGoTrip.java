/*
 * SeqGoTrip.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip;
import au.id.micolous.metrodroid.util.StationTableReader;

import java.util.GregorianCalendar;

/**
 * Represents trip events on Go Card.
 */
public class SeqGoTrip extends NextfareTrip {

    public static final Parcelable.Creator<SeqGoTrip> CREATOR = new Parcelable.Creator<SeqGoTrip>() {

        public SeqGoTrip createFromParcel(Parcel in) {
            return new SeqGoTrip(in);
        }

        public SeqGoTrip[] newArray(int size) {
            return new SeqGoTrip[size];
        }
    };

    /* Hard coded station IDs for Airtrain; used in tests */
    public static final int DOMESTIC_AIRPORT = 9;
    public static final int INTERNATIONAL_AIRPORT = 10;
    private static final String TAG = SeqGoTrip.class.getSimpleName();
    public static final String SEQ_GO_STR = "seq_go";

    /**
     * This constructor is used for unit tests outside of the package
     *
     * @param startStation Starting station ID.
     * @param endStation   Ending station ID.
     * @param startTime    Start time of the journey.
     * @param endTime      End time of the journey.
     * @param journeyId    Journey ID.
     * @param continuation True if this is a continuation of a previous journey (transfer).
     */
    public SeqGoTrip(int startStation, int endStation, GregorianCalendar startTime, GregorianCalendar endTime, int journeyId, boolean continuation) {
        this();
        mStartStation = startStation;
        mEndStation = endStation;
        mStartTime = startTime;
        mEndTime = endTime;
        mJourneyId = journeyId;
        mContinuation = continuation;
    }

    public SeqGoTrip() {
        super("AUD");
    }

    public SeqGoTrip(Parcel in) {
        super(in);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        switch (mMode) {
            case FERRY:
                return "Transdev Brisbane Ferries";
            case TRAIN:
                if (mStartStation == DOMESTIC_AIRPORT ||
                        mEndStation == DOMESTIC_AIRPORT ||
                        mStartStation == INTERNATIONAL_AIRPORT ||
                        mEndStation == INTERNATIONAL_AIRPORT) {
                    return "Airtrain";
                } else {
                    return "Queensland Rail";
                }
            default:
                return "TransLink";
        }
    }

    @Override
    public Station getStartStation() {
        if (mStartStation <= 0) {
            return null;
        }

        return StationTableReader.getStation(SEQ_GO_STR, mStartStation);
    }

    @Override
    public Station getEndStation() {
        if (mEndStation <= 0) {
            return null;
        }

        return StationTableReader.getStation(SEQ_GO_STR, mEndStation);
    }

    @Override
    public Mode getMode() {
        return mMode;
    }

    public int getJourneyId() {
        return mJourneyId;
    }

    @Override
    public String getRouteLanguage() {
        return "en-AU";
    }
}
