/*
 * Trip.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit

import android.os.Build
import android.os.Parcelable
import android.support.annotation.StringRes
import android.support.annotation.VisibleForTesting
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.LocaleSpan
import android.text.style.TtsSpan
import android.util.Log

import java.util.Calendar
import java.util.HashSet
import java.util.Locale

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.ui.HiddenSpan
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils

abstract class Trip : Parcelable {

    /**
     * Starting time of the trip.
     */
    abstract val startTimestamp: Calendar?

    /**
     * Ending time of the trip. If this is not known, return null.
     *
     * This returns null if not overridden in a subclass.
     */
    open val endTimestamp: Calendar?
        get() = null

    /**
     * Route name for the trip. This could be a bus line, a tram line, a rail line, etc.
     * If this is not known, then return null.
     *
     * The default implementation attempts to get the route name based on the
     * [.getStartStation] and [.getEndStation], using the
     * [Station.getLineNames] method.
     *
     * It does this by attempting to find a common set of Line Names between the Start and End
     * stations.
     *
     * If there is no start or end station data available, or [Station.getLineNames] returns
     * null, then this also returns null.
     *
     * If getting for display purposes, use [.getRouteDisplayName] instead.
     */
    open val routeName: String?
        @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
        get() {
            val startStation = startStation
            val endStation = endStation

            val startLines = startStation?.lineNames ?: emptyList()
            val endLines = endStation?.lineNames ?: emptyList()

            return getRouteName(startLines, endLines)
        }

    /**
     * Route IDs for the trip. This could be a bus line, a tram line, a rail line, etc.
     * If this is not known, then return null.
     *
     * The default implementation attempts to get the route name based on the
     * [.getStartStation] and [.getEndStation], using the
     * [Station.getHumanReadableLineIDs] method.
     *
     * It does this by attempting to find a common set of Line Names between the Start and End
     * stations.
     *
     * If there is no start or end station data available, or
     * [Station.getHumanReadableLineIDs] returns null, then this also returns null.
     */
    open val humanReadableRouteID: String?
        get() {
            val startStation = startStation
            val endStation = endStation

            val startLines = startStation?.humanReadableLineIDs ?: emptyList()
            val endLines = endStation?.humanReadableLineIDs ?: emptyList()

            return getRouteName(startLines, endLines)
        }

    /**
     * Get the route name for display purposes.
     *
     * This handles the "showRawStationIds" setting.
     */
    // Likely unknown route
    // Known route
    // No ID
    // No Name
    // Only do this if the raw display is on, because the route name may be hidden
    // because it is zero.
    open val routeDisplayName: String?
        get() {
            val routeName = routeName
            val routeID = humanReadableRouteID

            return if (Preferences.showRawStationIds) {
                if (routeName != null) {
                    if (routeID != null) {
                        if (routeName.contains(routeID)) {
                            routeName
                        } else {
                            String.format(Locale.ENGLISH, "%s [%s]", routeName, routeID)
                        }
                    } else {
                        routeName
                    }
                } else {
                    routeID
                }
            } else routeName

        }

    /**
     * Language that the route name is written in. This is used to aid text to speech software
     * with pronunciation. If null, then uses the system language instead.
     */
    open val routeLanguage: String?
        get() = null

    /**
     * Vehicle number where the event was recorded.
     *
     * This is generally *not* the Station ID, which is declared in
     * [.getStartStation].
     *
     * This is *not* the Farebox or Machine number.
     */
    open val vehicleID: String?
        get() = null

    /**
     * Machine ID that recorded the transaction. A machine in this context is a farebox, ticket
     * machine, or ticket validator.
     *
     * This is generally *not* the Station ID, which is declared in
     * [.getStartStation].
     *
     * This is *not* the Vehicle number.
     */
    open val machineID: String?
        get() = null

    /**
     * Number of passengers.
     *
     * -1 is unknown or irrelevant (eg: ticket machine purchases).
     */
    open val passengerCount: Int
        get() = -1

    /**
     * Starting station info for the trip, or null if there is no station information available.
     *
     * If supplied, this will be used to render a map of the trip.
     *
     * If there is station information available on the card, but the station is unknown (maybe it
     * doesn't appear in a MdST file, or there is no MdST data available yet), use
     * [Station.unknown] or [Station.unknown] to create an unknown
     * [Station].
     */
    open val startStation: Station?
        get() = null

    /**
     * Ending station info for the trip, or null if there is no station information available.
     *
     * If supplied, this will be used to render a map of the trip.
     *
     * If there is station information available on the card, but the station is unknown (maybe it
     * doesn't appear in a MdST file, or there is no MdST data available yet), use
     * [Station.unknown] or [Station.unknown] to create an unknown
     * [Station].
     */
    open val endStation: Station?
        get() = null

    /**
     * Formats the cost of the trip in the appropriate local currency.  Be aware that your
     * implementation should use language-specific formatting and not rely on the system language
     * for that information.
     *
     *
     * For example, if a phone is set to English and travels to Japan, it does not make sense to
     * format their travel costs in dollars.  Instead, it should be shown in Yen, which the Japanese
     * currency formatter does.
     *
     * @return The cost of the fare formatted in the local currency of the card.
     */
    abstract val fare: TransitCurrency?

    abstract val mode: Mode

    /**
     * If the trip is a transfer from another service, return true.
     *
     * If this is not a transfer, or this is unknown, return false. By default, this method returns
     * false.
     *
     * The typical use of this is if an agency allows you to transfer to other services within a
     * time window.  eg: The first trip is $2, but other trips within the next two hours are free.
     *
     * This may still return true even if [.getFare] is non-null and non-zero -- this
     * can indicate a discounted (rather than free) transfer. eg: The trips are $2.50, but other
     * trips within the next two hours have a $2 discount, making the second trip cost $0.50.
     */
    open val isTransfer: Boolean
        get() = false

    /**
     * If the tap-on event was rejected for the trip, return true.
     *
     * This should be used for where a record is added to the card in the case of insufficient
     * funds to pay for the journey.
     *
     * Otherwise, return false.  The default is to return false.
     */
    open val isRejected: Boolean
        get() = false

    /**
     * Full name of the agency for the trip. This is used on the map of the trip, where there is
     * space for longer agency names.
     *
     * If this is not known (or there is only one agency for the card), then return null.
     *
     * By default, this returns null.
     *
     * When isShort is true it means to return short name for trip list where space is limited.
     */
    open fun getAgencyName(isShort: Boolean): String? {
        return null
    }

    /**
     * Some cards don't store the exact time of day for each transaction, and only store the date.
     *
     *
     * If true, then a time should be shown next to the transaction in the history view. If false,
     * then the time of day will be hidden.
     *
     *
     * Trips are always sorted by the startTimestamp (including time of day), regardless of the
     * value given here.
     *
     * @return true if a time of day should be displayed.
     */
    open fun hasTime(): Boolean {
        return true
    }

    /**
     * Is there geographic data associated with this trip?
     */
    open fun hasLocation(): Boolean {
        val startStation = startStation
        val endStation = endStation
        return startStation != null && startStation.hasLocation() || endStation != null && endStation.hasLocation()
    }

    enum class Mode private constructor(val imageResourceIdx: Int, @param:StringRes @field:StringRes
    @get:StringRes
    val description: Int) {
        BUS(0, R.string.mode_bus),
        /** Used for non-metro (rapid transit) trains  */
        TRAIN(1, R.string.mode_train),
        /** Used for trams and light rail  */
        TRAM(2, R.string.mode_tram),
        /** Used for electric metro and subway systems  */
        METRO(3, R.string.mode_metro),
        FERRY(4, R.string.mode_ferry),
        TICKET_MACHINE(5, R.string.mode_ticket_machine),
        VENDING_MACHINE(6, R.string.mode_vending_machine),
        /** Used for transactions at a store, buying something other than travel.  */
        POS(7, R.string.mode_pos),
        OTHER(8, R.string.mode_unknown),
        BANNED(9, R.string.mode_banned),
        TROLLEYBUS(10, R.string.mode_trolleybus)
    }

    class Comparator : java.util.Comparator<Trip> {
        override fun compare(trip1: Trip, trip2: Trip): Int {
            val t1 = if (trip1.startTimestamp != null) trip1.startTimestamp else trip1.endTimestamp
            val t2 = if (trip2.startTimestamp != null) trip2.startTimestamp else trip2.endTimestamp
            return if (t2 != null && t1 != null) {
                t2.compareTo(t1)
            } else if (t2 != null) {
                1
            } else {
                0
            }
        }
    }

    companion object {
        private val TAG = Trip::class.java.name

        /**
         * Formats a trip description into a localised label, with appropriate language annotations.
         *
         * @param trip The trip to describe.
         * @return null if both the start and end stations are unknown.
         */
        fun formatStationNames(trip: Trip): Spannable? {
            var startStationName: String? = null
            var endStationName: String? = null
            var startLanguage: String? = null
            var endLanguage: String? = null
            val localisePlaces = Preferences.localisePlaces

            if (trip.startStation != null) {
                startStationName = trip.startStation!!.shortStationName
                startLanguage = trip.startStation!!.language
            }

            if (trip.endStation != null && (trip.startStation == null || trip.endStation!!.stationName != trip.startStation!!.stationName)) {
                endStationName = trip.endStation!!.shortStationName
                endLanguage = trip.endStation!!.language
            }

            // No information is available.
            if (startStationName == null && endStationName == null) {
                return null
            }

            // If only the start station is available, just return that.
            if (startStationName != null && endStationName == null) {
                val b = SpannableStringBuilder(startStationName)

                if (localisePlaces && startLanguage != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    b.setSpan(LocaleSpan(Locale.forLanguageTag(startLanguage)), 0, b.length, 0)
                }

                return b
            }

            // Both the start and end station are known.
            val startPlaceholder = "%1\$s"
            val endPlaceholder = "%2\$s"
            var s = Localizer.localizeString(R.string.trip_description, startPlaceholder, endPlaceholder)

            if (startStationName == null) {
                s = Localizer.localizeString(R.string.trip_description_unknown_start, endPlaceholder)
            }

            // Build the spans
            val b = SpannableStringBuilder(s)

            // Find the TTS-exclusive bits
            // They are wrapped in parentheses: ( )
            var x = 0
            while (x < b.toString().length) {
                val start = b.toString().indexOf("(", x)
                if (start == -1) break
                var end = b.toString().indexOf(")", start)
                if (end == -1) break

                // Delete those characters
                b.delete(end, end + 1)
                b.delete(start, start + 1)

                // We have a range, create a span for it
                b.setSpan(HiddenSpan(), start, --end, 0)

                x = end
            }

            // Find the display-exclusive bits.
            // They are wrapped in square brackets: [ ]
            x = 0
            while (x < b.toString().length) {
                val start = b.toString().indexOf("[", x)
                if (start == -1) break
                var end = b.toString().indexOf("]", start)
                if (end == -1) break

                // Delete those characters
                b.delete(end, end + 1)
                b.delete(start, start + 1)
                end--

                // We have a range, create a span for it
                // This only works properly on Lollipop. It's a pretty reasonable target for
                // compatibility, and most TTS software will not speak out Unicode arrows anyway.
                //
                // This works fine with Talkback, but *doesn't* work with Select to Speak.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    b.setSpan(TtsSpan.TextBuilder().setText(" ").build(), start, end, 0)
                }

                x = end
            }
            var localeSpanUsed: Boolean

            if (startStationName != null) {
                // Finally, insert the actual station names back in the data.
                x = b.toString().indexOf(startPlaceholder)
                if (x == -1) {
                    Log.w(TAG, "couldn't find start station placeholder to put back")
                    return null
                }
                b.replace(x, x + startPlaceholder.length, startStationName)

                localeSpanUsed = false
                // Annotate the start station name with the appropriate Locale data.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val startStation = trip.startStation
                    if (localisePlaces && startStation != null && startStation.language != null) {
                        b.setSpan(LocaleSpan(Locale.forLanguageTag(startStation.language)), x, x + startStationName.length, 0)

                        // Set the start of the string to the default language, so that the localised
                        // TTS for the station name doesn't take over everything.
                        b.setSpan(LocaleSpan(Locale.getDefault()), 0, x, 0)

                        localeSpanUsed = true
                    }
                }
            } else {
                localeSpanUsed = true
                x = 0
            }

            val y = b.toString().indexOf(endPlaceholder)
            if (y == -1) {
                Log.w(TAG, "couldn't find end station placeholder to put back")
                return null
            }
            b.replace(y, y + endPlaceholder.length, endStationName)

            // Annotate the end station name with the appropriate Locale data.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val endStation = trip.endStation
                if (localisePlaces) {
                    if (endStation != null && endStation.language != null) {
                        b.setSpan(LocaleSpan(Locale.forLanguageTag(endStation.language)), y, y + endStationName!!.length, 0)

                        if (localeSpanUsed) {
                            // Set the locale of the string between the start and end station names.
                            b.setSpan(LocaleSpan(Locale.getDefault()), x + startStationName!!.length, y, 0)
                        } else {
                            // Set the locale of the string from the start of the string to the end station
                            // name.
                            b.setSpan(LocaleSpan(Locale.getDefault()), 0, y, 0)
                        }

                        // Set the segment from the end of the end station name to the end of the string
                        b.setSpan(LocaleSpan(Locale.getDefault()), y + endStationName.length, b.length, 0)
                    } else {
                        // No custom language information for end station
                        // Set default locale from the end of the start station to the end of the string.
                        b.setSpan(LocaleSpan(Locale.getDefault()), x + startStationName!!.length, b.length, 0)
                    }
                }
            }

            return b
        }

        fun getRouteName(startLines: List<String>,
                         endLines: List<String>): String? {
            if (startLines.isEmpty() && endLines.isEmpty()) {
                return null
            }

            // Method 1: if only the start is set, use the first start line.
            if (endLines.isEmpty()) {
                return startLines[0]
            }

            // Method 2: if only the end is set, use the first end line.
            if (startLines.isEmpty()) {
                return endLines[0]
            }

            // Now there is at least 1 candidate line from each group.

            // Method 3: get the intersection of the two list of candidate stations
            val lines = HashSet(startLines)
            lines.retainAll(endLines)
            if (!lines.isEmpty()) {
                // There is exactly 1 common line -- return it
                if (lines.size == 1) {
                    return lines.iterator().next()
                }

                // There are more than one common line. Return the first one that appears in the order
                // of the starting line stations.
                for (candidateLine in startLines) {
                    if (lines.contains(candidateLine)) {
                        return candidateLine
                    }
                }
            }

            // There are no overlapping lines. Return the first associated with the start station.
            return startLines[0]
        }
    }
}
