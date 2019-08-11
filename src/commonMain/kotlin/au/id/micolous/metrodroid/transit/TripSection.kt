/*
 * TripSection.kt
 *
 * Copyright 2019 Google
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

import au.id.micolous.metrodroid.time.Timestamp

data class TripSection internal constructor(val trips: List<Trip>, val date: Timestamp? = getDate(trips[0])) {
    companion object {
        private fun getDate(trip: Trip): Timestamp? = trip.startTimestamp ?: trip.endTimestamp
        private fun shouldSplit(trips: List<Trip>, position: Int): Boolean {
            if (position == 0) return true

            val date1 = trips[position].let { getDate(it) }
            val date2 = trips[position - 1]?.let { getDate(it) }

            if (date1 == null && date2 != null) return true
            return if (date1 == null || date2 == null) false else !date1.isSameDay(date2)
        }

        fun sectionize(trips: List<Trip>): List<TripSection> {
            if (trips.isEmpty())
              return emptyList()
            val res = mutableListOf<TripSection>()
            var current = mutableListOf<Trip>()
            for (position in trips.indices) {
                if (shouldSplit(trips, position) && current.isNotEmpty()) {
                    res.add(TripSection(current))
                    current = mutableListOf()
                }
                current.add(trips[position])
            }
            if (current.isNotEmpty()) {
                res.add(TripSection(current))
            }
            return res
        }
    }
}
