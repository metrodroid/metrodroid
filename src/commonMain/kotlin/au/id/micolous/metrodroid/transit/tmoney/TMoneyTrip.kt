/*
 * TmoneyTrip.java
 *
 * Copyright 2018 Google
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

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class TMoneyTrip (private val mTime: Long,
                  private val mCost: Int,
                  private val mType: Int): Trip() {

    override val fare: TransitCurrency?
        get() = TransitCurrency.KRW(mCost)

    override val mode: Trip.Mode
        get() = when (mType) {
            2 -> Trip.Mode.TICKET_MACHINE
            else -> Trip.Mode.OTHER
        }
    
    override val startTimestamp: Timestamp?
        get() = parseHexDateTime(mTime)

    companion object {
        private val TZ = MetroTimeZone.SEOUL
        private const val INVALID_DATETIME = 0xffffffffffffffL

        private fun parseHexDateTime(value: Long): Timestamp? =
                if (value == INVALID_DATETIME) null else TimestampFull(TZ,
                    NumberUtils.convertBCDtoInteger((value shr 40).toInt()),
                    NumberUtils.convertBCDtoInteger((value shr 32 and 0xffL).toInt()) - 1,
                    NumberUtils.convertBCDtoInteger((value shr 24 and 0xffL).toInt()),
                    NumberUtils.convertBCDtoInteger((value shr 16 and 0xffL).toInt()),
                    NumberUtils.convertBCDtoInteger((value shr 8 and 0xffL).toInt()),
                    NumberUtils.convertBCDtoInteger((value and 0xffL).toInt()))

        fun parseTrip(data: ImmutableByteArray): TMoneyTrip? {
            val type: Int = data[0].toInt()
            var cost: Int
            val time: Long = data.byteArrayToLong(26, 7)
            // 1 byte type
            // 1 byte unknown
            // 4 bytes balance after transaction
            // 4 bytes counter
            // 4 bytes cost
            cost = data.byteArrayToInt(10, 4)
            if (type == 2)
                cost = -cost
            // 2 bytes unknown
            // 1 byte type??
            // 7 bytes unknown
            // 7 bytes time
            // 7 bytes zero
            // 4 bytes unknown
            // 2 bytes zero
            if (cost == 0 && time == INVALID_DATETIME) return null
            return TMoneyTrip(mType = type, mCost = cost, mTime = time)
        }
    }
}
