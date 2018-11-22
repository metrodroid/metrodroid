/*
 * TripObfuscator.java
 *
 * Copyright 2017-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.util;

import android.util.Log;

import au.id.micolous.metrodroid.transit.Trip;

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

    private static final SecureRandom mRNG = new SecureRandom();

    /**
     * Remaps days of the year to a different day of the year.
     */
    private static final List<Integer> mCalendarMapping = new ArrayList<>();

    static {
        // Generate list of ints from 0-366 (each day in year).
        for (int x = 0; x < 366; x++) {
            mCalendarMapping.add(x);
        }

        Collections.shuffle(mCalendarMapping);
    }

    /**
     * Maybe obfuscates a timestamp
     *
     * @param input          Calendar representing the time to obfuscate
     * @param obfuscateDates true if dates should be obfuscated
     * @param obfuscateTimes true if times should be obfuscated
     * @return maybe obfuscated value
     */
    private static Calendar maybeObfuscateTS(Calendar input, boolean obfuscateDates, boolean obfuscateTimes) {
        if (!obfuscateDates && !obfuscateTimes) {
            return input;
        }

        if (input == null) {
            return null;
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

    public static Calendar maybeObfuscateTS(Calendar input) {
        return maybeObfuscateTS(input, MetrodroidApplication.obfuscateTripDates(),
                MetrodroidApplication.obfuscateTripTimes());
    }

    public static List<Trip> obfuscateTrips(List<Trip> trips, boolean obfuscateDates, boolean obfuscateTimes, boolean obfuscateFares) {
        List<Trip> newTrips = new ArrayList<>();
        for (Trip trip : trips) {
            Calendar start = trip.getStartTimestamp();
            long timeDelta = 0;

            if (start != null) {
                timeDelta = maybeObfuscateTS(start, obfuscateDates, obfuscateTimes).getTimeInMillis() - start.getTimeInMillis();
            } else {
                timeDelta = 0;
            }


            newTrips.add(new ObfuscatedTrip(trip, timeDelta, obfuscateFares));
        }
        return newTrips;
    }
}
