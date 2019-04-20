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
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.Utils
import java.util.*

class OctopusData {
    companion object {
        val OCTOPUS_TZ = MetroTimeZone.BEIJING

        private val OCTOPUS_OFFSETS = listOf(
                TimestampFull(year = 1997, month = 0, day = 1, hour = 0, min = 0, tz = OCTOPUS_TZ) to 350,

                // Negative balance amount changes, which changes the offset:
                // https://www.octopus.com.hk/en/consumer/customer-service/faq/get-your-octopus/about-octopus.html#3532
                // https://www.octopus.com.hk/en/consumer/customer-service/faq/get-your-octopus/about-octopus.html#3517
                TimestampFull(year = 2017, month = 10, day = 1, hour = 0, min = 0, tz = OCTOPUS_TZ) to 500
        )

        private const val SHENZHEN_OFFSET = 350

        // Shenzhen Tong issues different cards now, so do not know if the new balance applies to
        // that card as well.

        private fun getOffset(scanTime: TimestampFull, offsets: List<Pair<TimestampFull, Int>>) : Int {
            var offset = offsets.first().second

            for (it in offsets) {
                if (scanTime > it.first) {
                    offset = it.second
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
