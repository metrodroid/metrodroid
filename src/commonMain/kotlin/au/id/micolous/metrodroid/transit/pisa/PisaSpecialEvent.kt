/*
 * PisaSpecialEvent.java
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
data class PisaSpecialEvent internal constructor(override val parsed: En1545Parsed) : En1545Transaction() {
    override val lookup get() = PisaLookup

    companion object {
        fun parse(data: ImmutableByteArray) = PisaSpecialEvent(En1545Parser.parse(data, tripFields))

        private val tripFields = En1545Container(
                En1545FixedInteger(EVENT_UNKNOWN_A, 13),
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timeLocal(EVENT),
                En1545FixedHex(EVENT_UNKNOWN_B, 0x1d * 8 - 14 - 11 - 13)
        )
    }
}
