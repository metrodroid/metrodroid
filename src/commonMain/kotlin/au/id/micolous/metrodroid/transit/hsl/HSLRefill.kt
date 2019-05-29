/*
 * HSLRefill.kt
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
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
package au.id.micolous.metrodroid.transit.hsl

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class HSLRefill (override val startTimestamp: Timestamp?,
                 private val mRefillAmount: Int): Trip() {
    override val fare: TransitCurrency?
        get() = TransitCurrency.EUR(-mRefillAmount)

    override val mode: Trip.Mode
        get() = Trip.Mode.TICKET_MACHINE

    constructor(data: ImmutableByteArray) : this(
        startTimestamp = HSLTransitData.cardDateToCalendar(
                data.getBitsFromBuffer(20, 14),
                data.getBitsFromBuffer(34, 11)),
        mRefillAmount = data.getBitsFromBuffer(45, 20))

    override fun getAgencyName(isShort: Boolean) = Localizer.localizeString(R.string.hsl_balance_refill)
}
