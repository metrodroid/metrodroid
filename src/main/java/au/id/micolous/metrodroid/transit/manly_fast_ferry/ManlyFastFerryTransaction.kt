/*
 * ManlyFastFerryTransaction.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

import java.util.GregorianCalendar

import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.erg.ErgTransaction
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord

class ManlyFastFerryTransaction(purse: ErgPurseRecord, epoch: GregorianCalendar?) :
        ErgTransaction(purse, epoch, ManlyFastFerryTransitData.CURRENCY) {

    // All transactions look the same... but this is a ferry, so we'll call it a ferry one.
    // Even when you buy things at the cafe.
    override fun getMode() = Trip.Mode.FERRY
}
