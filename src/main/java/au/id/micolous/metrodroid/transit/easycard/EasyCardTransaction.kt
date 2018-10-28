/*
 * EasyCardTransaction.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
 *
 * Based on code from:
 * - http://www.fuzzysecurity.com/tutorials/rfid/4.html
 * - Farebot <https://codebutler.github.io/farebot/>
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
package au.id.micolous.metrodroid.transit.easycard

import android.support.annotation.VisibleForTesting
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransitData.Companion.EASYCARD_STR
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class EasyCardTransaction internal constructor(
        internal val timestamp: Long,
        private var fare: Int,
        private val location: Int,
        private val isTapOff: Boolean,
        private val machineId: Long,
        private var exitTimestamp: Long? = null,
        private var exitLocation: Int? = null
) : Trip() {
    @VisibleForTesting
    constructor(data: ByteArray) : this(
            Utils.byteArrayToLongReversed(data, 1, 4),
            Utils.byteArrayToIntReversed(data, 6, 2),
            data[11].toInt(),
            data[5] == 0x11.toByte(),
            Utils.byteArrayToLongReversed(data, 12, 4)
    )

    override fun getFare(): TransitCurrency? = TransitCurrency.TWD(fare)

    override fun getStartTimestamp(): Calendar? = EasyCardTransitData.parseTimestamp(timestamp)

    override fun getEndTimestamp(): Calendar? = EasyCardTransitData.parseTimestamp(exitTimestamp)

    override fun getStartStation(): Station? = when (location) {
        BUS -> null
        POS -> null
        else -> StationTableReader.getStation(EASYCARD_STR, location)
    }

    override fun getEndStation(): Station? {
        return StationTableReader.getStation(EASYCARD_STR, exitLocation ?: return null)
    }

    override fun getMode(): Mode {
        return when (location) {
            BUS -> Mode.BUS
            POS -> Mode.POS
            else -> Mode.METRO
        }
    }

    override fun getMachineID(): String? = "0x" + machineId.toString(16)

    fun shouldBeMerged(trip: EasyCardTransaction): Boolean {
        if (location == POS || location == BUS
                || trip.location == POS || trip.location == BUS
                || trip.exitTimestamp != null || exitTimestamp != null) {
            return false
        }
        return (!isTapOff && trip.isTapOff)
    }

    fun merge(trip: EasyCardTransaction) {
        fare += trip.fare
        exitLocation = trip.location
        exitTimestamp = trip.timestamp
    }

    override fun getRouteName(): String? = when (mode) {
        Mode.METRO -> super.getRouteName()
        // Ticket machines would otherwise inherit a line from the Station.
        else -> null
    }

    companion object {
        internal const val POS = 1
        internal const val BUS = 5

        internal fun parseTrips(card: ClassicCard): List<EasyCardTransaction> {
            val blocks = (
                    (card.getSector(3) as ClassicSector).blocks.subList(1, 3) +
                            (card.getSector(4) as ClassicSector).blocks.subList(0, 3) +
                            (card.getSector(5) as ClassicSector).blocks.subList(0, 3))
                    .filter { !it.data.all { it == 0x0.toByte() } }

            var trips = blocks.map { block ->
                EasyCardTransaction(block.data)
            }.distinctBy { it.timestamp }

            Collections.sort(trips, Trip.Comparator())
            Collections.reverse(trips)

            var mergedTrips = ArrayList<EasyCardTransaction>()

            for (trip in trips) {
                if (mergedTrips.isEmpty()) {
                    mergedTrips.add(trip)
                    continue
                }
                var lastTrip = mergedTrips.get(mergedTrips.size - 1)
                if (lastTrip.shouldBeMerged(trip))
                    lastTrip.merge(trip)
                else
                    mergedTrips.add(trip)
            }

            return mergedTrips
        }
    }
}
