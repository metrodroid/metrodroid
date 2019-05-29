/*
 * OpusTrip.kt
 *
 * Copyright 2018 Etienne Dubeau
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

package au.id.micolous.metrodroid.transit.opus

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
internal data class OpusTransaction(override val parsed: En1545Parsed) : En1545Transaction() {
    override val lookup: En1545Lookup
        get() = OpusLookup.instance

    constructor(data: ImmutableByteArray) : this(parsed = En1545Parser.parse(data, tripFields))

    companion object {
        private val tripFields = En1545Container(
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timeLocal(EVENT),
                En1545FixedInteger("UnknownX", 19), // Possibly part of following bitmap
                En1545Bitmap(
                        En1545FixedInteger(EVENT_UNKNOWN_A, 8),
                        En1545FixedInteger(EVENT_UNKNOWN_B, 8),
                        En1545FixedInteger(EVENT_SERVICE_PROVIDER, 8),
                        En1545FixedInteger(EVENT_UNKNOWN_C, 16),
                        En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
                        // How 32 bits are split among next 2 fields is unclear
                        En1545FixedInteger(EVENT_UNKNOWN_D, 16),
                        En1545FixedInteger(EVENT_UNKNOWN_E, 16),
                        En1545FixedInteger(EVENT_CONTRACT_POINTER, 5),
                        En1545Bitmap(
                                En1545FixedInteger.date(EVENT_FIRST_STAMP),
                                En1545FixedInteger.timeLocal(EVENT_FIRST_STAMP),
                                En1545FixedInteger("EventDataSimulation", 1),
                                En1545FixedInteger(EVENT_UNKNOWN_F, 4),
                                En1545FixedInteger(EVENT_UNKNOWN_G, 4),
                                En1545FixedInteger(EVENT_UNKNOWN_H, 4),
                                En1545FixedInteger(EVENT_UNKNOWN_I, 4)
                        )
                )
        )
    }
}
