/*
 * LeapTransitData.java
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.transit.tfi_leap

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.TimestampFull

internal fun<T> valuesCompatible(a: T?, b: T?): Boolean =
        (a == null || b == null || a == b)

@Parcelize
internal class LeapTripPoint (val mTimestamp: TimestampFull?,
                              val mAmount: Int?,
                              private val mEventCode: Int?,
                              val mStation: Int?): Parcelable {
    fun isMergeable(other: LeapTripPoint?): Boolean =
        other == null || (
                valuesCompatible(mAmount, other.mAmount) &&
                        valuesCompatible(mTimestamp, other.mTimestamp) &&
                        valuesCompatible(mEventCode, other.mEventCode) &&
                        valuesCompatible(mStation, other.mStation)
                )

    companion object {
        fun merge(a: LeapTripPoint?, b: LeapTripPoint?) = LeapTripPoint(
                mAmount = a?.mAmount ?: b?.mAmount,
                mTimestamp = a?.mTimestamp ?: b?.mTimestamp,
                mEventCode = a?.mEventCode ?: b?.mEventCode,
                mStation = a?.mStation ?: b?.mStation
        )
    }
}
