/*
 * ClipperUltralightTrip.kt
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

package au.id.micolous.metrodroid.transit.clipper

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class ClipperUltralightTrip (private val mTime: Int,
                             private val mTransferExpiry: Int,
                             private val mSeqCounter: Int,
                             val tripsRemaining: Int,
                             private val mBalanceSeqCounter: Int,
                             private val mStation: Int,
                             private val mType: Int,
                             private val mAgency: Int): Trip() {
    val isHidden: Boolean
        get() = mType == 1

    override val startStation: Station?
        get() = ClipperData.getStation(mAgency, mStation, false)

    override val startTimestamp: Timestamp?
        get() = ClipperTransitData.clipperTimestampToCalendar(mTime * 60L)

    val transferExpiry: Int
        get() = if (mTransferExpiry == 0) 0 else mTransferExpiry + mTime

    override val fare: TransitCurrency?
        get() = null

    override val mode: Trip.Mode
        get() = ClipperData.getMode(mAgency)

    constructor(transaction: ImmutableByteArray, baseDate: Int) : this (
        mSeqCounter = transaction.getBitsFromBuffer(0, 7),
        mType = transaction.getBitsFromBuffer(7, 17),
        mTime = baseDate * 1440 - transaction.getBitsFromBuffer(24, 17),
        mStation = transaction.getBitsFromBuffer(41, 17),
        mAgency = transaction.getBitsFromBuffer(68, 5),
        mBalanceSeqCounter = transaction.getBitsFromBuffer(80, 4),
        tripsRemaining = transaction.getBitsFromBuffer(84, 6),
        mTransferExpiry = transaction.getBitsFromBuffer(100, 10)
        // Last 4 bytes are hash
    )

    override fun getAgencyName(isShort: Boolean) =
        ClipperData.getAgencyName(mAgency, isShort)

    fun isSeqGreater(other: ClipperUltralightTrip): Boolean {
        return if (other.mBalanceSeqCounter != mBalanceSeqCounter) mBalanceSeqCounter - other.mBalanceSeqCounter and 0x8 == 0 else mSeqCounter - other.mSeqCounter and 0x40 == 0
    }
}
