/*
 * TMoneyTrip.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.tmoney

import au.id.micolous.metrodroid.card.ksx6924.KSX6924Utils.INVALID_DATETIME
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Utils.parseHexDateTime
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.android.parcel.Parcelize

@Parcelize
open class TMoneyTrip(open val type: Int, open val cost: Int, open val time: Long) : Trip() {

    override val fare
        get() = TransitCurrency.KRW(cost)

    override val mode: Mode
        get() = when (type) {
            2 -> Mode.TICKET_MACHINE
            else -> Mode.OTHER
        }

    override val startTimestamp: TimestampFull?
        get() = parseHexDateTime(time, TZ)

    companion object {
        private val TZ = MetroTimeZone.SEOUL

        fun parseTrip(data: ImmutableByteArray): TMoneyTrip? {
            // 1 byte type
            val type = data[0].toInt()
            // 1 byte unknown
            // 4 bytes balance after transaction
            val balance = data.byteArrayToInt(2, 4)
            // 4 bytes counter
            // 4 bytes cost
            var cost = data.byteArrayToInt(10, 4)
            if (type == 2)
                cost = -cost
            // 2 bytes unknown
            // 1 byte type??
            // 7 bytes unknown
            // 7 bytes time
            val time = data.byteArrayToLong(26, 7)
            // 7 bytes zero
            // 4 bytes unknown
            // 2 bytes zero

            return if (cost == 0 && time == INVALID_DATETIME) null else TMoneyTrip(type, cost, time)
        }
    }
}
