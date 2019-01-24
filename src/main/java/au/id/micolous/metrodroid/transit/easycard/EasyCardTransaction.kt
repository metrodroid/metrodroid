/*
 * EasyCardTransaction.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class EasyCardTransaction internal constructor(
        internal val timestamp: Long,
        private val fare: Int,
        private val location: Int,
        private val isEndTap: Boolean,
        private val machineId: Long
) : Transaction() {
    @VisibleForTesting
    constructor(data: ImmutableByteArray) : this(
            data.byteArrayToLongReversed(1, 4),
            data.byteArrayToIntReversed(6, 2),
            data[11].toInt(),
            data[5] == 0x11.toByte(),
            data.byteArrayToLongReversed(12, 4)
    )

    override fun getFare() = TransitCurrency.TWD(fare)

    override fun getTimestamp() = EasyCardTransitData.parseTimestamp(timestamp)

    override fun getStation(): Station? = when (location) {
        BUS -> null
        POS -> null
        else -> StationTableReader.getStation(EasyCardTransitData.EASYCARD_STR, location)
    }

    override fun getMode() = when (location) {
        BUS -> Trip.Mode.BUS
        POS -> Trip.Mode.POS
        else -> Trip.Mode.METRO
    }

    override fun getMachineID() = "0x${machineId.toString(16)}"

    override fun isSameTrip(trip: Transaction): Boolean {
        if (trip !is EasyCardTransaction) {
            return false
        }

        if (location == POS || location == BUS
                || trip.location == POS || trip.location == BUS) {
            return false
        }

        return (!isEndTap && trip.isEndTap)
    }

    override fun isTapOff() = isEndTap

    override fun isTapOn() = !isEndTap

    override fun getRouteNames(): List<String> = when (mode) {
        Trip.Mode.METRO -> super.getRouteNames()
        else -> Collections.emptyList()
    }

    companion object {
        internal const val POS = 1
        internal const val BUS = 5

        internal fun parseTrips(card: ClassicCard): List<Trip> {
            val blocks = (
                    card[3].blocks.subList(1, 3) +
                            card[4].blocks.subList(0, 3) +
                            card[5].blocks.subList(0, 3))
                    .filter { !it.data.isAllZero() }

            val trips = blocks.map { block ->
                EasyCardTransaction(block.data)
            }.distinctBy { it.timestamp }

            return TransactionTrip.merge(trips)
        }
    }
}
