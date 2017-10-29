package com.codebutler.farebot.util;

import android.util.Log;

import com.codebutler.farebot.transit.Trip;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import au.id.micolous.metrodroid.MetrodroidApplication;

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

    /**
     * Maybe obfuscates a timestamp
     * @param input Calendar representing the time to obfuscate
     * @param obfuscateDates true if dates should be obfuscated
     * @param obfuscateTimes true if times should be obfuscated
     * @return maybe obfuscated value
     */
    public static Calendar maybeObfuscateTS(Calendar input, boolean obfuscateDates, boolean obfuscateTimes) {
        if (!obfuscateDates && !obfuscateTimes) {
            return input;
        }

        int today = GregorianCalendar.getInstance().get(Calendar.DAY_OF_YEAR);

        // Clone the input before we start messing with it.
        Calendar newDate = GregorianCalendar.getInstance();
        newDate.setTimeInMillis(input.getTimeInMillis());

        if (obfuscateDates) {
            int dayOfYear = newDate.get(Calendar.DAY_OF_YEAR);
            if (dayOfYear < mCalendarMapping.size()) {
                dayOfYear = mCalendarMapping.get(dayOfYear);
            } else {
                // Shouldn't happen...
                Log.w(TAG, String.format("Oops, got out of range day-of-year (%d)", dayOfYear));
            }

            newDate.set(Calendar.DAY_OF_YEAR, dayOfYear);

            // Adjust for the time of year
            if (dayOfYear >= today) {
                newDate.add(Calendar.YEAR, -1);
            }
        }

        if (obfuscateTimes) {
            // Reduce resolution of timestamps to 5 minutes.
            newDate.setTimeInMillis((newDate.getTimeInMillis() / 300000) * 300000);

            // Add a deviation of up to 20,000 seconds (5.5 hours) earlier or later.
            newDate.add(Calendar.SECOND, mRNG.nextInt(40000) - 20000);
        }

        return newDate;
    }

    /**
     * Maybe obfuscates a timestamp
     * @param input seconds since UNIX epoch (1970-01-01)
     * @param obfuscateDates true if dates should be obfuscated
     * @param obfuscateTimes true if times should be obfuscated
     * @return maybe obfuscated value
     */
    public static long maybeObfuscateTS(long input, boolean obfuscateDates, boolean obfuscateTimes) {
        if (!obfuscateDates && !obfuscateTimes) {
            return input;
        }

        Calendar s = GregorianCalendar.getInstance();
        s.setTimeInMillis(UNIX_EPOCH.getTimeInMillis() + (input * 1000));

        return maybeObfuscateTS(s, obfuscateDates, obfuscateTimes).getTimeInMillis() / 1000;
    }

    public static long maybeObfuscateTS(long input) {
        return maybeObfuscateTS(input, MetrodroidApplication.obfuscateTripDates(),
                MetrodroidApplication.obfuscateTripTimes());
    }

    public static Calendar maybeObfuscateTS(Calendar input) {
        return maybeObfuscateTS(input, MetrodroidApplication.obfuscateTripDates(),
                MetrodroidApplication.obfuscateTripTimes());
    }


    public static TripObfuscator getInstance() {
        return singleton;
    }

    public static List<Trip> obfuscateTrips(List<Trip> trips, boolean obfuscateDates, boolean obfuscateTimes, boolean obfuscateFares) {
        List<Trip> newTrips = new ArrayList<>();
        for (Trip trip : trips) {
            long start = trip.getTimestamp();
            long timeDelta = 0;
            int fareOffset = 0;
            double fareMultiplier = 1.0;

            timeDelta = maybeObfuscateTS(start, obfuscateDates, obfuscateTimes) - start;

            if (obfuscateFares) {
                // These are unique for each fare
                fareOffset = mRNG.nextInt(100) - 50;

                // Multiplier may be 0.8 ~ 1.2
                fareMultiplier = (mRNG.nextDouble() * 0.4) + 0.8;
            }

            newTrips.add(new ObfuscatedTrip(trip, timeDelta, fareOffset, fareMultiplier));
        }
        return newTrips;
    }
}
