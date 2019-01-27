/*
 * CharlieCardTrip.java
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

package au.id.micolous.metrodroid.transit.charlie

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class CharlieCardTrip (private val mFare: Int,
                       private val mValidator: Int,
                       private val mTimestamp: Int): Trip() {

    override val startStation: Station?
        get() = Station.unknown(mValidator shr 3)

    override val startTimestamp: TimestampFull?
        get() = CharlieCardTransitData.parseTimestamp(mTimestamp)

    override val fare: TransitCurrency?
        get() = TransitCurrency.USD(mFare)

    override val mode: Trip.Mode
        get() {
            when (mValidator and 7) {
                0 -> return Trip.Mode.TICKET_MACHINE
                1 -> return Trip.Mode.BUS
            }
            return Trip.Mode.OTHER
        }

    constructor(data: ImmutableByteArray, off: Int): this(
        mFare = CharlieCardTransitData.getPrice(data, off + 5),
        mValidator = data.byteArrayToInt(off + 3, 2),
        mTimestamp = data.byteArrayToInt(off, 3))
}
