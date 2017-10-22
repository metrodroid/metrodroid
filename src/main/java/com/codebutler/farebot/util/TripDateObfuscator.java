package com.codebutler.farebot.util;

import android.os.Parcel;
import android.util.Log;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Obfuscates trip dates
 */

public final class TripDateObfuscator {
    private static final String TAG = "TripDateObfuscator";
    private static final GregorianCalendar UNIX_EPOCH = new GregorianCalendar(1970, Calendar.JANUARY, 1);

    private static final TripDateObfuscator singleton = new TripDateObfuscator();

    private static List<Integer> mCalendarMapping = new ArrayList<>();

    static {
        // Generate list of ints from 0-366 (each day in year).
        for (int x=0; x<366; x++) {
            mCalendarMapping.add(x);
        }

        Collections.shuffle(mCalendarMapping);
    }

    private class ObfuscatedTrip extends Trip {
        private Trip mRealTrip;
        private long mDelta;

        ObfuscatedTrip(Trip realTrip, long delta) {
            mRealTrip = realTrip;
            mDelta = delta;
        }

        @Override
        public long getTimestamp() {
            if (mRealTrip.getTimestamp() == 0) {
                return 0;
            }

            return mRealTrip.getTimestamp() + mDelta;
        }

        @Override
        public long getExitTimestamp() {
            if (mRealTrip.getExitTimestamp() == 0) {
                return 0;
            }

            return mRealTrip.getExitTimestamp() + mDelta;
        }

        @Override
        public String getRouteName() {
            return mRealTrip.getRouteName();
        }

        @Override
        public String getAgencyName() {
            return mRealTrip.getAgencyName();
        }

        @Override
        public String getShortAgencyName() {
            return mRealTrip.getShortAgencyName();
        }

        @Override
        public String getBalanceString() {
            return mRealTrip.getBalanceString();
        }

        @Override
        public String getStartStationName() {
            return mRealTrip.getStartStationName();
        }

        @Override
        public Station getStartStation() {
            return mRealTrip.getStartStation();
        }

        @Override
        public String getEndStationName() {
            return mRealTrip.getEndStationName();
        }

        @Override
        public Station getEndStation() {
            return mRealTrip.getEndStation();
        }

        @Override
        public boolean hasFare() {
            return mRealTrip.hasFare();
        }

        @Override
        public String getFareString() {
            return mRealTrip.getFareString();
        }

        @Override
        public Mode getMode() {
            return mRealTrip.getMode();
        }

        @Override
        public boolean hasTime() {
            return mRealTrip.hasTime();
        }

        @Override
        public int describeContents() {
            return mRealTrip.describeContents();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            mRealTrip.writeToParcel(parcel, i);
        }
    }

    public static List<Trip> obfuscateTrips(List<Trip> trips) {
        return getInstance().obfuscateTripsImpl(trips);
    }

    public static TripDateObfuscator getInstance() {
        return singleton;
    }

    private List<Trip> obfuscateTripsImpl(List<Trip> trips) {
        List<Trip> newTrips = new ArrayList<>();
        int today = GregorianCalendar.getInstance().get(Calendar.DAY_OF_YEAR);
        for (Trip trip : trips) {
            long start = trip.getTimestamp();

            Calendar startCalendar = GregorianCalendar.getInstance();
            startCalendar.setTimeInMillis(UNIX_EPOCH.getTimeInMillis() + (start * 1000));

            int dayOfYear = startCalendar.get(Calendar.DAY_OF_YEAR);
            if (dayOfYear < mCalendarMapping.size()) {
                dayOfYear = mCalendarMapping.get(dayOfYear);
            } else {
                // Shouldn't happen...
                Log.w(TAG, String.format("Oops, got out of range day-of-year (%d)", dayOfYear));
            }

            startCalendar.set(Calendar.DAY_OF_YEAR, dayOfYear);

            // Adjust for the time of year
            if (dayOfYear >= today) {
                startCalendar.add(Calendar.YEAR, -1);
            }

            long delta = ((startCalendar.getTimeInMillis() - UNIX_EPOCH.getTimeInMillis()) / 1000) - start;

            newTrips.add(new ObfuscatedTrip(trip, delta));
        }
        return newTrips;
    }
}
