package com.codebutler.farebot.util;

import android.util.Log;

import com.codebutler.farebot.transit.Trip;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Obfuscates trip dates
 */

public final class TripObfuscator {
    private static final String TAG = "TripObfuscator";
    private static final GregorianCalendar UNIX_EPOCH = new GregorianCalendar(1970, Calendar.JANUARY, 1);

    private static final TripObfuscator singleton = new TripObfuscator();

    private static final SecureRandom mRNG = new SecureRandom();

    /**
     * Remaps days of the year to a different day of the year.
     */
    private static List<Integer> mCalendarMapping = new ArrayList<>();

    static {
        // Generate list of ints from 0-366 (each day in year).
        for (int x=0; x<366; x++) {
            mCalendarMapping.add(x);
        }

        Collections.shuffle(mCalendarMapping);
    }


    public static TripObfuscator getInstance() {
        return singleton;
    }

    public static List<Trip> obfuscateTrips(List<Trip> trips, boolean obfuscateDates, boolean obfuscateTimes, boolean obfuscateFares, boolean obfuscateBalance) {
        List<Trip> newTrips = new ArrayList<>();
        int today = GregorianCalendar.getInstance().get(Calendar.DAY_OF_YEAR);
        for (Trip trip : trips) {
            long timeDelta = 0;
            int fareOffset = 0;
            double fareMultiplier = 1.0;


            if (obfuscateDates) {
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

                timeDelta = ((startCalendar.getTimeInMillis() - UNIX_EPOCH.getTimeInMillis()) / 1000) - start;
            }

            if (obfuscateFares) {
                // This is unique for each fare, but hopefully we'll only generate this once.
                fareOffset = mRNG.nextInt(100) - 50;

                // Multiplier may be 0.8 ~ 1.2, but applied consistently
                fareMultiplier = (mRNG.nextDouble() * 0.4) + 0.8;
            }

            newTrips.add(new ObfuscatedTrip(trip, timeDelta, fareOffset, fareMultiplier));
        }
        return newTrips;
    }
}
