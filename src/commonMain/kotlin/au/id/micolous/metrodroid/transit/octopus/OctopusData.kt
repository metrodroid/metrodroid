/*
 * OctopusData.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.octopus

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Month
import au.id.micolous.metrodroid.time.TimestampFull

class OctopusData {
    companion object {
        private val OCTOPUS_TZ = MetroTimeZone.BEIJING

        private val OCTOPUS_OFFSETS = listOf(
                TimestampFull(OCTOPUS_TZ,
                    1997, Month.JANUARY, 1, 0, 0) to 350,

                // Negative balance amount change effective 2017-10-01, which changes the offset:
                // https://www.octopus.com.hk/en/consumer/customer-service/faq/get-your-octopus/about-octopus.html#3532
                // https://www.octopus.com.hk/en/consumer/customer-service/faq/get-your-octopus/about-octopus.html#3517
                TimestampFull(OCTOPUS_TZ,
                    2017, Month.OCTOBER, 1, 0, 0) to 500
        )

        private const val SHENZHEN_OFFSET = 350

        // Shenzhen Tong issues different cards now, so do not know if the new balance applies to
        // that card as well.

        private fun getOffset(scanTime: TimestampFull, offsets: List<Pair<TimestampFull, Int>>) : Int {
            var offset = offsets.first().second

            for ((offsetStart, offsetValue) in offsets) {
                if (scanTime > offsetStart) {
                    offset = offsetValue
                } else {
                    break
                }
            }

            return offset
        }

        fun getOctopusOffset(scanTime: TimestampFull) = getOffset(scanTime, OCTOPUS_OFFSETS)

        fun getShenzhenOffset(@Suppress("UNUSED_PARAMETER") scanTime: TimestampFull) = SHENZHEN_OFFSET
    }
}
