/*
 * TripObfuscator.kt
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
package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.Trip
import kotlin.random.Random

/**
 * Obfuscates trip dates
 */

object TripObfuscator {
    private const val TAG = "TripObfuscator"

    /**
     * Remaps days of the year to a different day of the year.
     */
    private val mCalendarMapping = (0..365).shuffled()

    private fun obfuscateDaystamp(input: Daystamp): Daystamp {
        var year = input.year
        var dayOfYear = input.dayOfYear
        if (dayOfYear < mCalendarMapping.size) {
            dayOfYear = mCalendarMapping[dayOfYear]
        } else {
            // Shouldn't happen...
            Log.w(TAG, "Oops, got out of range day-of-year ($dayOfYear)")
        }

        val today = TimestampFull.now().toDaystamp()

        // Adjust for the time of year
        if (year > today.year || year == today.year && dayOfYear >= today.dayOfYear) {
            year--
        }

        return Daystamp.fromDayOfYear(year, dayOfYear)
    }

    /**
     * Maybe obfuscates a timestamp
     *
     * @param input          Calendar representing the time to obfuscate
     * @param obfuscateDates true if dates should be obfuscated
     * @param obfuscateTimes true if times should be obfuscated
     * @return maybe obfuscated value
     */
    private fun maybeObfuscateTSFull(input: TimestampFull, obfuscateDates: Boolean, obfuscateTimes: Boolean): TimestampFull {
        if (!obfuscateDates && !obfuscateTimes) {
            return input
        }

        // Clone the input before we start messing with it.
        val daystamp = maybeObfuscateTSDay(
            input.toDaystamp(), obfuscateDates)

        if (!obfuscateTimes)
            return daystamp.promote(input.tz, input.hour, input.minute)

        // Reduce resolution of timestamps to 5 minutes.
        val minute = (input.minute + 2) / 5 * 5

        // Add a deviation of up to 350 minutes (5.5 hours) earlier or later.
        val off = Random.nextInt(700) - 350

        return daystamp.promote(input.tz, input.hour, minute) + Duration.mins(off)
    }

    private fun maybeObfuscateTSDay(input: Daystamp, obfuscateDates: Boolean): Daystamp {
        if (!obfuscateDates) {
            return input
        }

        return obfuscateDaystamp(input)
    }

    fun maybeObfuscateTS(input: TimestampFull): TimestampFull =
            maybeObfuscateTSFull(input, Preferences.obfuscateTripDates,
                Preferences.obfuscateTripTimes)

    fun maybeObfuscateTS(input: Daystamp): Daystamp =
            maybeObfuscateTSDay(input, Preferences.obfuscateTripDates)

    private fun obfuscateTrip(trip: Trip, obfuscateDates: Boolean, obfuscateTimes: Boolean, obfuscateFares: Boolean): Trip {
        val start = trip.startTimestamp
        val timeDelta: Long = when (start) {
            null -> 0
            is TimestampFull -> maybeObfuscateTSFull(start, obfuscateDates, obfuscateTimes).timeInMillis - start.timeInMillis
            is Daystamp -> 86400L * 1000L * (maybeObfuscateTSDay(start, obfuscateDates).daysSinceEpoch - start.daysSinceEpoch).toLong()
        }

        return ObfuscatedTrip(trip, timeDelta, obfuscateFares)
    }

    fun obfuscateTrips(trips: List<Trip>, obfuscateDates: Boolean, obfuscateTimes: Boolean, obfuscateFares: Boolean): List<Trip> =
            trips.map { obfuscateTrip(it, obfuscateDates, obfuscateTimes, obfuscateFares) }
}
