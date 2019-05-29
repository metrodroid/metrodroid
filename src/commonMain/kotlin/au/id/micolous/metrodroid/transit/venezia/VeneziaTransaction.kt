/*
 * RavKavTrip.kt
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
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

abstract class VeneziaTransaction : En1545Transaction() {
    override val lookup get() = VeneziaLookup

    override val mode: Trip.Mode
        get() {
            when (parsed.getInt(TRANSPORT_TYPE)) {
                1 -> return Trip.Mode.BUS
                5 -> return Trip.Mode.FERRY
            }
            if (parsed.getInt(Y_VALUE) == 1000)
                return Trip.Mode.FERRY
            return Trip.Mode.BUS
        }

    companion object {
        const val TRANSPORT_TYPE = "TransportType"
        const val Y_VALUE = "Y"
    }
}

@Parcelize
data class VeneziaTransactionCalypso internal constructor(override val parsed: En1545Parsed) : VeneziaTransaction() {
    companion object {
        fun parse(data: ImmutableByteArray) =
                if (data.sliceOffLen(9, data.size - 9).isAllZero())
                    null
                else
                    VeneziaTransactionCalypso(En1545Parser.parse(data, tripFields))

        private fun contractsElement(id: Int) = En1545Container(
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_A + "$id", 1),
                En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF + "$id", 16),
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_B + "$id", 1)
        )

        private val tripFields = En1545Container(
                contractsElement(1),
                contractsElement(2),
                contractsElement(3),
                contractsElement(4),
                En1545FixedInteger(EVENT_UNKNOWN_A, 1),
                En1545FixedInteger(EVENT_CONTRACT_TARIFF,16),
                En1545FixedInteger(EVENT_UNKNOWN_B, 4),
                En1545FixedInteger.datePacked(EVENT),
                En1545FixedInteger.timePacked11Local(EVENT_FIRST_STAMP),
                En1545FixedInteger.timePacked11Local(EVENT),
                En1545FixedInteger(EVENT_UNKNOWN_C, 9),
                En1545FixedInteger(TRANSPORT_TYPE, 4),
                En1545FixedInteger(Y_VALUE, 14),
                En1545FixedInteger("Z", 16),
                En1545FixedInteger(EVENT_UNKNOWN_E, 18),
                En1545FixedInteger("PreviousZ", 16),
                En1545FixedInteger(EVENT_UNKNOWN_F, 26)
        )
    }
}
