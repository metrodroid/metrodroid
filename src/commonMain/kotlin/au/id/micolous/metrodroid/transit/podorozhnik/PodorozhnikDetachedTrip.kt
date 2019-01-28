/*
 * Copyright (c) 2018 Google
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

package au.id.micolous.metrodroid.transit.podorozhnik

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip

@Parcelize
internal class PodorozhnikDetachedTrip (private val mTimestamp: Int): Trip() {

    override val startTimestamp: Timestamp?
        get() = PodorozhnikTransitData.convertDate(mTimestamp)

    override val fare: TransitCurrency?
        get() = null

    override val mode: Trip.Mode
        get() = Trip.Mode.OTHER
}
