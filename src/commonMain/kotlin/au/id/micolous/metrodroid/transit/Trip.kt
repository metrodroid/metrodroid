/*
 * Trip.kt
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

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.Preferences

abstract class Trip : Parcelable {
    /**
     * Starting time of the trip.
     */
    abstract val startTimestamp: Timestamp?

    /**
     * Ending time of the trip. If this is not known, return null.
     *
     * This returns null if not overridden in a subclass.
     */
    open val endTimestamp: Timestamp?
        get() = null

    /**
     * Route name for the trip. This could be a bus line, a tram line, a rail line, etc.
     * If this is not known, then return null.
     *
     * The default implementation attempts to get the route name based on the
     * {@link #getStartStation()} and {@link #getEndStation()}, using the
     * {@link Station#getLineNames()} method.
     *
     * It does this by attempting to find a common set of Line Names between the Start and End
     * stations.
     *
     * If there is no start or end station data available, or {@link Station#getLineNames()} returns
     * null, then this also returns null.
     *
     * If getting for display purposes, use {@link #getRouteDisplayName()} instead.
     */
    open val routeName: String?
        get() {
            val startLines = startStation?.lineNames.orEmpty()
            val endLines = endStation?.lineNames.orEmpty()
            return getRouteName(startLines, endLines)
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
    open fun getAgencyName(isShort: Boolean): String? = null

    /**
     * Is there geographic data associated with this trip?
     */
    fun hasLocation(): Boolean {
        val startStation = startStation
        val endStation = endStation
        return startStation != null && startStation.hasLocation() || endStation != null && endStation.hasLocation()
    }

    enum class Mode(val idx: Int, val description: StringResource) {
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
        TROLLEYBUS(10, R.string.mode_trolleybus),
        TOLL_ROAD(11, R.string.mode_toll_road),
    }

    class Comparator : kotlin.Comparator<Trip> {
        private fun getSortingKey(t: Trip): Long? {
            val s = t.startTimestamp
            val e = t.endTimestamp
            val x = when {
                s is TimestampFull -> s
                e is TimestampFull -> e
                s != null -> s
                else -> e
            }
            return when (x) {
                null -> null
                is TimestampFull -> x.timeInMillis
                is Daystamp -> x.daysSinceEpoch * 86400L * 1000L
            }
        }

        override fun compare(a: Trip, b: Trip): Int {
            val t1 = getSortingKey(a)
            val t2 = getSortingKey(b)
            return if (t2 != null && t1 != null) {
                t2.compareTo(t1)
            } else if (t2 != null) {
                1
            } else {
                0
            }
        }
    }


    /**
     * Route IDs for the trip. This could be a bus line, a tram line, a rail line, etc.
     * If this is not known, then return null.
     *
     * The default implementation attempts to get the route name based on the
     * {@link #getStartStation()} and {@link #getEndStation()}, using the
     * {@link Station#getHumanReadableLineIDs()} method.
     *
     * It does this by attempting to find a common set of Line Names between the Start and End
     * stations.
     *
     * If there is no start or end station data available, or
     * {@link Station#getHumanReadableLineIDs()} returns null, then this also returns null.
     */
    open val humanReadableRouteID: String?
        get() {
            val startLines = startStation?.humanReadableLineIds ?: emptyList()
            val endLines = endStation?.humanReadableLineIds ?: emptyList()
            return getRouteName(startLines, endLines)
        }

    open fun getRawFields(level: TransitData.RawLevel): String? = null

    companion object {
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
            val lines = startLines.toSet() intersect endLines.toSet()
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

        /**
         * Get the route name for display purposes.
         *
         * This handles the "showRawStationIds" setting.
         */
        fun getRouteDisplayName(trip: Trip): String? {
            if (!Preferences.showRawStationIds)
                return trip.routeName
            val routeName = trip.routeName
            val routeID = trip.humanReadableRouteID ?: return routeName

            if (routeName == null) {
                // No Name
                // Only do this if the raw display is on, because the route name may be hidden
                // because it is zero.
                return routeID
            }

            return if (routeName.contains(routeID)) {
                // Likely unknown route
                routeName
            } else {
                // Known route
                "$routeName [$routeID]"
            }
        }
    }
}
