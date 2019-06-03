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
internal class MobibTransaction(override val parsed: En1545Parsed) : En1545Transaction() {
    override val station: Station?
        get() {
            val agency = parsed.getIntOrZero(En1545Transaction.EVENT_SERVICE_PROVIDER)
            return if (agency == MobibLookup.BUS || agency == MobibLookup.TRAM) StationTableReader.getStation(MOBIB_STR,
                    parsed.getIntOrZero(En1545Transaction.EVENT_ROUTE_NUMBER) shl 13
                            or parsed.getIntOrZero(EVENT_LOCATION_ID_BUS) or (agency shl 22)) else super.station
        }

    override val lookup: En1545Lookup
        get() = MobibLookup

    val transactionNumber: Int
        get() = parsed.getIntOrZero(EVENT_SERIAL_NUMBER)

    companion object {
            private const val EVENT_LOCATION_ID_BUS = "EventLocationIdBus"
            private const val EVENT_VERSION = "EventVersion"
            fun parse(data: ImmutableByteArray): MobibTransaction? {
                    if (data.isAllZero())
                    return null
                    val version = data.getBitsFromBuffer(0, 6)

                    val fields = when {
                            version <= 2 -> En1545Container(
                                    En1545FixedInteger(EVENT_VERSION, 6),
                                    En1545FixedInteger.date(EVENT),
                                    En1545FixedInteger.time(EVENT),
                                    En1545FixedInteger(EVENT_UNKNOWN_B, 21),
                                    En1545FixedInteger(EVENT_PASSENGER_COUNT, 5),
                                    En1545FixedInteger(EVENT_UNKNOWN_C, 14),
                                    En1545FixedInteger(EVENT_LOCATION_ID_BUS, 12), // Curious
                                    En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
                                    En1545FixedInteger(EVENT_SERVICE_PROVIDER, 5), // Curious
                                    En1545FixedInteger(EVENT_LOCATION_ID, 17), // Curious
                                    En1545FixedInteger(EVENT_UNKNOWN_E, 10),
                                    En1545FixedInteger(EVENT_UNKNOWN_F, 7),
                                    En1545FixedInteger(EVENT_SERIAL_NUMBER, 24),
                                    En1545FixedInteger("EventTransferNumber", 24),
                                    En1545FixedInteger.date(EVENT_FIRST_STAMP),
                                    En1545FixedInteger.time(EVENT_FIRST_STAMP),
                                    En1545FixedInteger(EVENT_UNKNOWN_G, 21)
                            )
                            else -> En1545Container(
                                    En1545FixedInteger(EVENT_VERSION, 6), // confirmed
                                    En1545FixedInteger.date(EVENT), // confirmed
                                    En1545FixedInteger.time(EVENT), // confirmed
                                    En1545FixedInteger(EVENT_UNKNOWN_B + "1", 31),
                                    En1545Bitmap(
                                            En1545Container(
                                                    En1545FixedInteger(EVENT_UNKNOWN_B + "2", 4), 
                                                    En1545FixedInteger(EVENT_LOCATION_ID_BUS, 12) // confirmed
                                            ),
                                            En1545FixedInteger(EVENT_ROUTE_NUMBER, 16), // confirmed
                                            En1545FixedInteger("NeverSeen2", 16),
                                            En1545FixedInteger("NeverSeen3", 16),
                                            En1545Container(
                                                    En1545FixedInteger(EVENT_SERVICE_PROVIDER, 5), // confirmed
                                                    En1545FixedInteger(EVENT_LOCATION_ID, 17), // confirmed
                                                    En1545FixedInteger(EVENT_UNKNOWN_E + "1", 10)
                                            )
                                    ),
                                    En1545Bitmap(
                                            En1545FixedInteger(EVENT_SERIAL_NUMBER, 24), // confirmed
                                            En1545FixedInteger(EVENT_UNKNOWN_F, 16), // zero?
                                            En1545FixedInteger("EventTransferNumber", 8), // zero?
                                            En1545FixedInteger("NeverSeenA3", 16),
                                            En1545Container(
                                                    En1545FixedInteger.date(EVENT_FIRST_STAMP), // confirmed
                                                    En1545FixedInteger.time(EVENT_FIRST_STAMP) // confirmed
                                            ),
                                            En1545FixedInteger("NeverSeenA5", 16)
                                    ),
                                    En1545FixedInteger(EVENT_UNKNOWN_G, 21)
                            )
                    }
                    return MobibTransaction(En1545Parser.parse(data, fields))
            }
    }
}
