/*
 * PisaTrip.java
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.transit.pisa

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class PisaTransaction internal constructor(override val parsed: En1545Parsed) : En1545Transaction() {
    override val lookup get() = PisaLookup

    companion object {
        fun parse(data: ImmutableByteArray) = PisaTransaction(En1545Parser.parse(data, tripFields))
        fun parseUltralight(data: ImmutableByteArray) =
                if (data.byteArrayToInt(0, 4) == 0)
                    null
                else PisaTransaction(En1545Parser.parse(data, tripULFields))

        private val tripFields = En1545Container(
                En1545FixedHex(EVENT_UNKNOWN_A, 71),
                En1545FixedInteger.dateTimeLocal(EVENT),
                En1545FixedHex(EVENT_UNKNOWN_B, 82),
                En1545FixedInteger.dateTimeLocal(EVENT_FIRST_STAMP),
                En1545FixedInteger("ValueA", 16),
                En1545FixedHex(EVENT_UNKNOWN_C, 92),
                En1545FixedInteger("ValueB", 16),
                En1545FixedHex(EVENT_UNKNOWN_D, 47)
        )

        private val tripULFields = En1545Container(
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timeLocal(EVENT),
                En1545FixedInteger(EVENT_UNKNOWN_A, 18),
                En1545FixedInteger("ValueB", 16),
                En1545FixedInteger("ValueA", 16),
                En1545FixedHex(EVENT_UNKNOWN_B, 37),
                En1545FixedInteger(EVENT_AUTHENTICATOR, 16)
        )
    }
}
