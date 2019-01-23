/*
 * RkfTripLeg.kt
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

import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class RkfTripLeg(private val mStartTimestamp: Calendar,
                      private val mEndTimestamp: Calendar?,
                      private val mStartStation: Station?,
                      private val mEndStation: Station?,
                      private val mFare: TransitCurrency?,
                      private val mMode: Mode,
                      private val mPassengerCount: Int,
                      private val mShortAgencyName: String?,
                      private val mTransfer: Boolean,
                      private val mAgencyName: String?) : Trip() {
    override val startTimestamp get() = mStartTimestamp
    override val endTimestamp get() = mEndTimestamp
    override val startStation get() = mStartStation
    override val endStation get() = mEndStation
    override val fare get() = mFare
    override val passengerCount get() = mPassengerCount
    override val mode get() = mMode
    override fun getAgencyName(isShort: Boolean) = if (isShort) mShortAgencyName else mAgencyName
    override val isTransfer get() = mTransfer
}
