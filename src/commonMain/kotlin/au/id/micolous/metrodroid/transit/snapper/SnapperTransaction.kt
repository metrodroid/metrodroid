/*
 * SnapperTransaction.kt
 *
 * Copyright 2018 Google
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/Snapper
 */
package au.id.micolous.metrodroid.transit.snapper

import au.id.micolous.metrodroid.card.ksx6924.KSX6924Utils.parseHexDateTime
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Transient

@Parcelize
class SnapperTransaction(
        val journeyId: Int,
        val seq: Int,
        override val isTapOn: Boolean,
        val type: Int,
        val cost: Int,
        val time: Long,
        val operator: String) : Transaction() {


    @Transient
    override val isTapOff = !isTapOn

    // TODO: Implement this properly
    @Transient
    override val station = Station.nameOnly("$journeyId / $seq")

    @Transient
    override val mode = when (type) {
        2 -> Trip.Mode.BUS
        else -> Trip.Mode.TROLLEYBUS
    }

    override fun isSameTrip(other: Transaction): Boolean {
        val o = other as SnapperTransaction
        return journeyId == o.journeyId && seq == o.seq
    }

    @Transient
    override val timestamp = parseHexDateTime(time, TZ)

    @Transient
    override val fare = TransitCurrency.NZD(cost)

    @Transient
    override val isTransfer = seq != 0

    companion object {
        private val TZ = MetroTimeZone.AUCKLAND
        private const val TAG = "SnapperTransaction"

        fun parseTransaction(trip : ImmutableByteArray, balance : ImmutableByteArray) : SnapperTransaction {
            val journeyId = trip[5].toInt()
            val seq = trip[4].toInt()

            val time = trip.byteArrayToLong(13, 7)

            val tapOn = (trip[51].toInt() and 0x10) == 0x10

            val type = balance[0].toInt()
            val bal = balance.byteArrayToInt(2, 4)
            var cost = balance.byteArrayToInt(10, 4)
            if (type == 2)
                cost = -cost

            val operator = balance.sliceOffLen(14, 5).getHexString().substring(0, 9)

            Log.d(TAG, "Transaction: journey=$journeyId/$seq type=$type, cost=$cost, bal=$bal, time=$time, operator=$operator, tapOn=$tapOn")
            Log.d(TAG, " trip: ${trip.getHexString()}")
            Log.d(TAG, "  bal: ${balance.getHexString()}")

            return SnapperTransaction(journeyId, seq, tapOn, type, cost, time, operator)
        }
    }

}