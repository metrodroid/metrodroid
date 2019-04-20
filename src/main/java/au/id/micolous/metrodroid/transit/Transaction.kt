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

import android.os.Parcelable

import java.util.Calendar

abstract class Transaction : Parcelable, Comparable<Transaction> {
    protected abstract val isTapOff: Boolean

    /**
     * This method may be overridden to provide candidate line names associated with the
     * transaction. This is useful if there is a separate field on the card which encodes the route
     * or line taken, and that knowledge of the station alone is not generally sufficient to
     * determine the correct route.
     *
     * By default, this gets candidate route names from the Station.
     */
    open val routeNames: List<String>
        get() {
            val s = station
            return s?.lineNames ?: emptyList()
        }

    /**
     * This method may be overridden to provide candidate line names associated with the
     * transaction. This is useful if there is a separate field on the card which encodes the route
     * or line taken, and that knowledge of the station alone is not generally sufficient to
     * determine the correct route.
     *
     * By default, this gets candidate route names from the Station.
     */
    open val humanReadableLineIDs: List<String>
        get() {
            val s = station
            return s?.humanReadableLineIDs ?: emptyList()
        }

    open val vehicleID: String?
        get() = null

    open val machineID: String?
        get() = null

    open val passengerCount: Int
        get() = -1

    open val station: Station?
        get() = null

    abstract val timestamp: Calendar?

    abstract val fare: TransitCurrency

    open val mode: Trip.Mode
        get() = Trip.Mode.OTHER

    protected open val isCancel: Boolean
        get() = false

    protected abstract val isTapOn: Boolean

    protected open val isTransfer: Boolean
        get() = false

    protected open val isRejected: Boolean
        get() = false

    open fun getAgencyName(isShort: Boolean): String? {
        return null
    }

    protected open fun shouldBeMerged(other: Transaction): Boolean {
        return isTapOn && (other.isTapOff || other.isCancel) && isSameTrip(other)
    }

    protected abstract fun isSameTrip(other: Transaction): Boolean

    override fun compareTo(other: Transaction): Int {
        val t1 = timestamp
        val t2 = other.timestamp
        if (t1 == null && t2 == null)
            return 0
        if (t1 == null)
            return -1
        if (t2 == null)
            return +1
        return t1.compareTo(t2)
    }

    class Comparator : java.util.Comparator<Transaction> {
        override fun compare(txn1: Transaction, txn2: Transaction): Int {
            return txn1.compareTo(txn2)
        }
    }
}