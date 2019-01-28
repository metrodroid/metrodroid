/*
 * ChcMetrocardTrip.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.chc_metrocard

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.erg.ErgTrip
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord

@Parcelize
class ChcMetrocardTrip (override val purse: ErgPurseRecord,
                        override val epoch: Int): ErgTrip() {

    // There is a historic tram that circles the city, but not a commuter service, and does not
    // accept Metrocard.
    override val mode: Trip.Mode
        get() = Trip.Mode.BUS

    override val currency: String
        get() = ChcMetrocardTransitData.CURRENCY
    override val tz: MetroTimeZone
        get() = ChcMetrocardTransitData.TIME_ZONE
}
