/*
 * RkfTrip.kt
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

package au.id.micolous.metrodroid.transit.rkf

import android.os.Parcel
import android.os.Parcelable

import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.TransactionTrip
import au.id.micolous.metrodroid.transit.TransitCurrency

class RkfTrip : TransactionTrip {
    constructor(el: Transaction) : super(el)

    private constructor(source: Parcel) : super(source)

    override fun getFare(): TransitCurrency? {
        return TransitCurrency.sum(mStart?.fare, mEnd?.fare)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<RkfTrip> = object : Parcelable.Creator<RkfTrip> {
            override fun createFromParcel(source: Parcel): RkfTrip {
                return RkfTrip(source)
            }

            override fun newArray(size: Int): Array<RkfTrip?> {
                return arrayOfNulls(size)
            }
        }
    }
}
