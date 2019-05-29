/*
 * ClipperRefill.kt
 *
 * Copyright 2011 "an anonymous contributor"
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
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
package au.id.micolous.metrodroid.transit.clipper

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip

@Parcelize
class ClipperRefill (override val startTimestamp: TimestampFull?,
                     private val mAmount: Int,
                     private val mAgency: Int,
                     override val machineID: String?): Trip() {
    override val fare: TransitCurrency?
        get() = TransitCurrency.USD(-mAmount)

    override val mode: Trip.Mode
        get() = Trip.Mode.TICKET_MACHINE

    override fun getAgencyName(isShort: Boolean): String? =
            ClipperData.getAgencyName(mAgency, isShort)
}
