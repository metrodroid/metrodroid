/*
 * ManlyFastFerryTrip.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.manly_fast_ferry

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.erg.ErgTrip
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord

/**
 * Trips on the card are "purse debits", and it is not possible to tell it apart from non-ticket
 * usage (like cafe purchases).
 */
@Parcelize
class ManlyFastFerryTrip(override val purse: ErgPurseRecord,
                         override val epoch: Int) :
        ErgTrip() {
    override val currency: String
        get() = ManlyFastFerryTransitData.CURRENCY
    override val tz: MetroTimeZone
        get() = ManlyFastFerryTransitData.TIME_ZONE

    // All transactions look the same... but this is a ferry, so we'll call it a ferry one.
    // Even when you buy things at the cafe.
    override val mode: Trip.Mode
        get() = Trip.Mode.FERRY
}
