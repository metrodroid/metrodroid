/*
 * RavKavSubscription.java
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

package au.id.micolous.metrodroid.transit.venezia

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Duration
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class VeneziaSubscription(override val parsed: En1545Parsed, private val ctr: Int?) : En1545Subscription() {
    override val lookup get() = VeneziaLookup

    override val remainingTripCount get() = ctr?.div(256)

    companion object {
        fun parse(data: ImmutableByteArray, ctr: Int?) =
                if (data.getBitsFromBuffer(0, 22) == 0)
                    null
                else
                    VeneziaSubscription(En1545Parser.parse(data, SUB_FIELDS), ctr)

        private val SUB_FIELDS = En1545Container(
                En1545FixedInteger(CONTRACT_UNKNOWN_A, 6),
                En1545FixedInteger(CONTRACT_TARIFF, 16),
                En1545FixedInteger("IdCounter", 8),
                En1545FixedHex(CONTRACT_UNKNOWN_B, 82),
                En1545FixedInteger.datePacked(CONTRACT_SALE),
                En1545FixedInteger.timePacked11Local(CONTRACT_SALE)
                // Remainder: zero-filled
        )
    }
}
