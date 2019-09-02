/*
 * HafilatTransaction.kt
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

package au.id.micolous.metrodroid.transit.hafilat

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
open class HafilatTransaction (override val parsed: En1545Parsed): En1545Transaction() {
    override val lookup: En1545Lookup
        get() = HafilatLookup

    constructor(data: ImmutableByteArray) : this(En1545Parser.parse(data, tripFields))

    companion object {
        private val tripFields = En1545Container(
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timeLocal(EVENT),
                En1545Bitmap(
                        En1545FixedInteger(En1545Transaction.EVENT_DISPLAY_DATA, 8),
                        En1545FixedInteger(En1545Transaction.EVENT_NETWORK_ID, 24),
                        En1545FixedInteger(EVENT_CODE, 8),
                        En1545FixedInteger(En1545Transaction.EVENT_RESULT, 8),
                        En1545FixedInteger(En1545Transaction.EVENT_SERVICE_PROVIDER, 8),
                        En1545FixedInteger(En1545Transaction.EVENT_NOT_OK_COUNTER, 8),
                        En1545FixedInteger(En1545Transaction.EVENT_SERIAL_NUMBER, 24),
                        En1545FixedInteger(En1545Transaction.EVENT_DESTINATION, 16),
                        En1545FixedInteger(En1545Transaction.EVENT_LOCATION_ID, 16),
                        En1545FixedInteger(En1545Transaction.EVENT_LOCATION_GATE, 8),
                        En1545FixedInteger(En1545Transaction.EVENT_DEVICE, 16),
                        En1545FixedInteger(En1545Transaction.EVENT_ROUTE_NUMBER, 16),
                        En1545FixedInteger(En1545Transaction.EVENT_ROUTE_VARIANT, 8),
                        // Starting from here it seems to diverge from intercode
                        En1545FixedInteger(En1545Transaction.EVENT_VEHICLE_ID, 16),
                        En1545FixedInteger(En1545Transaction.EVENT_VEHICULE_CLASS, 8), // Class 4? Well could make sense as placeholder
                        En1545FixedInteger("B", 24),
                        En1545FixedInteger("NeverSeen16", 8),
                        En1545FixedInteger("NeverSeen17", 8),
                        En1545FixedInteger("NeverSeen18", 8),
                        En1545FixedInteger(En1545Transaction.EVENT_CONTRACT_POINTER, 5),
                        En1545FixedInteger("NeverSeen20", 8),
                        En1545FixedInteger("NeverSeen21", 8),
                        En1545FixedInteger("NeverSeen22", 8),
                        En1545FixedInteger("NeverSeen23", 8),
                        En1545FixedInteger("NeverSeen24", 8),
                        En1545FixedInteger("C", 8),
                        En1545FixedInteger("NeverSeen26", 8),
                        En1545FixedInteger(EVENT_AUTHENTICATOR, 16),
                        En1545Bitmap(
                                En1545FixedInteger.date(EVENT_FIRST_STAMP),
                                En1545FixedInteger.timeLocal(EVENT_FIRST_STAMP),
                                En1545FixedInteger(EVENT_DATA_SIMULATION, 1),
                                En1545FixedInteger(EVENT_DATA_TRIP, 2),
                                En1545FixedInteger(EVENT_DATA_ROUTE_DIRECTION, 2)
                        )
                )
        )
    }
}
