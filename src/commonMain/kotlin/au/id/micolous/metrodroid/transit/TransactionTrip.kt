/*
 * TransactionTrip.java
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

package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull

@Parcelize
class TransactionTripCapsule(var start: Transaction? = null,
                             var end: Transaction? = null): Parcelable

@Parcelize
class TransactionTrip(override val capsule: TransactionTripCapsule): TransactionTripAbstract() {
    companion object {
        fun merge(transactionsIn: List<Transaction>) =
                TransactionTripAbstract.merge(transactionsIn) { TransactionTrip(makeCapsule(it)) }

        fun merge(vararg transactions: Transaction): List<TransactionTripAbstract> =
                merge(transactions.toList())
    }
}

abstract class TransactionTripAbstract: Trip() {
    abstract val capsule: TransactionTripCapsule

    protected val start get() = capsule.start
    protected val end get() = capsule.end

    private val any: Transaction?
        get() = start ?: end

    override// Try to get the route from the nested transactions.
    // This automatically falls back to using the MdST.
    val routeName: String?
        get() {
            val startLines = start?.routeNames ?: emptyList()
            val endLines = end?.routeNames ?: emptyList()

            return Trip.getRouteName(startLines, endLines)
        }

    override// Try to get the route from the nested transactions.
    // This automatically falls back to using the MdST.
    val humanReadableRouteID: String?
        get() {
            val startLines = start?.humanReadableLineIDs ?: emptyList()
            val endLines = end?.humanReadableLineIDs ?: emptyList()

            return Trip.getRouteName(startLines, endLines)
        }

    override val passengerCount: Int
        get() = any?.passengerCount ?: -1

    override val vehicleID: String?
        get() = any?.vehicleID

    override val machineID: String?
        get() = any?.machineID

    override val startStation: Station?
        get() = start?.station

    override val endStation: Station?
        get() = end?.station

    override val startTimestamp: Timestamp?
        get() = start?.timestamp

    override val endTimestamp: Timestamp?
        get() = end?.timestamp

    override val mode: Trip.Mode
        get() = any?.mode ?: Trip.Mode.OTHER

    // No fare applies to the trip, as the tap-on was reversed.
    override val fare: TransitCurrency?
        get() {
            if (end?.isCancel == true) {
                return null
            }

            return end?.fare ?: start?.fare
        }

    override val isTransfer: Boolean
        get() = any?.isTransfer ?: false

    override val isRejected: Boolean
        get() = any?.isRejected ?: false

    override fun getAgencyName(isShort: Boolean): String? = any?.getAgencyName(isShort)

    companion object {
        fun makeCapsule(transaction: Transaction) : TransactionTripCapsule =
                if (transaction.isTapOff || transaction.isCancel)
                    TransactionTripCapsule(null, transaction)
                else
                    TransactionTripCapsule(transaction, null)

        fun merge(transactionsIn: List<Transaction>,
                  factory: (Transaction) -> TransactionTripAbstract):
                List<TransactionTripAbstract> {
            val timedTransactions = mutableListOf<Pair<Transaction, TimestampFull>>()
            val timelessTransactions = mutableListOf<Transaction>()
            for (transaction in transactionsIn) {
                val ts = transaction.timestamp
                if (ts is TimestampFull)
                    timedTransactions.add(Pair(transaction, ts))
                else
                    timelessTransactions.add(transaction)
            }
            val transactions = timedTransactions.sortedBy { it.second.timeInMillis }
            val trips = mutableListOf<TransactionTripAbstract>()
            for ((first) in transactions) {
                if (trips.isEmpty()) {
                    trips.add(factory(first))
                    continue
                }
                val previous = trips[trips.size - 1]
                if (previous.end == null && previous.start?.shouldBeMerged(first) == true)
                    previous.capsule.end = first
                else
                    trips.add(factory(first))
            }
            return trips + timelessTransactions.map { factory(it) }
        }
    }
}
