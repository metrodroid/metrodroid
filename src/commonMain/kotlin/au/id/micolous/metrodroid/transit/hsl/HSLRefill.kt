/*
 * HSLRefill.kt
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
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
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class HSLRefill private constructor(override val parsed: En1545Parsed): En1545Transaction() {
    override val lookup: En1545Lookup
        get() = HSLLookup
    override val fare: TransitCurrency?
        get() = super.fare?.negate()

    override val mode: Trip.Mode
        get() = Trip.Mode.TICKET_MACHINE

    override fun getAgencyName(isShort: Boolean) = Localizer.localizeFormatted(R.string.hsl_balance_refill)

    companion object {
        private val FIELDS_V1_V2 = En1545Container(
                En1545FixedInteger("CurrentValue", 20),
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timeLocal(EVENT),
                En1545FixedInteger(EVENT_PRICE_AMOUNT, 20),
                En1545FixedInteger("LoadingOrganisationID", 14),
                En1545FixedInteger(EVENT_DEVICE_ID, 14)
        )

        fun parse(data: ImmutableByteArray): HSLRefill? {
            val ret = En1545Parser.parse(data, FIELDS_V1_V2)
            if (ret.getIntOrZero(En1545FixedInteger.dateName(EVENT)) == 0)
                return null
            return HSLRefill(ret)
        }
    }
}
