/*
 * MobibTrip.kt
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

package au.id.micolous.metrodroid.transit.mobib

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
internal class MobibTransaction (override val parsed: En1545Parsed): En1545Transaction() {
    override val station: Station?
        get() {
            val agency = parsed.getIntOrZero(En1545Transaction.EVENT_SERVICE_PROVIDER)
            if (agency == TRAM)
                return null
            return if (agency == BUS) StationTableReader.getStation(MOBIB_STR,
                    parsed.getIntOrZero(En1545Transaction.EVENT_ROUTE_NUMBER) shl 13
                            or parsed.getIntOrZero(EVENT_LOCATION_ID_BUS) or (agency shl 22)) else super.station
        }

    override val lookup: En1545Lookup
        get() = MobibLookup

    constructor(data: ImmutableByteArray) : this(En1545Parser.parse(data, FIELDS))

    companion object {
        private const val TRAM = 0x16
        private const val BUS = 0xf

        private const val EVENT_LOCATION_ID_BUS = "EventLocationIdBus"
        private val FIELDS = En1545Container(
                En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_A, 6),
                En1545FixedInteger.date(En1545Transaction.EVENT),
                En1545FixedInteger.time(En1545Transaction.EVENT),
                En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_B, 21),
                En1545FixedInteger(En1545Transaction.EVENT_PASSENGER_COUNT, 5),
                En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_C, 14),
                En1545FixedInteger(EVENT_LOCATION_ID_BUS, 12), // Curious
                En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_D, 9),
                En1545FixedInteger(En1545Transaction.EVENT_ROUTE_NUMBER, 7), // curious
                En1545FixedInteger(En1545Transaction.EVENT_SERVICE_PROVIDER, 5), // Curious
                En1545FixedInteger(En1545Transaction.EVENT_LOCATION_ID, 17), // Curious
                En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_E, 10),
                En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_F, 7),
                En1545FixedInteger(En1545Transaction.EVENT_SERIAL_NUMBER, 24),
                En1545FixedInteger("EventTransferNumber", 24),
                En1545FixedInteger.date(En1545Transaction.EVENT_FIRST_STAMP),
                En1545FixedInteger.time(En1545Transaction.EVENT_FIRST_STAMP),
                En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_G, 21)
        )
    }
}
