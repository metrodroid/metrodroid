/*
 * NewShenzhenTrip.kt
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

package au.id.micolous.metrodroid.transit.china

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class ChinaTripCapsule(val mTime: Long,
                            val mCost: Int,
                            val mType: Int,
                            val mStation: Long): Parcelable {
    constructor(data: ImmutableByteArray) : this(
        // 2 bytes counter
        // 3 bytes zero
        // 4 bytes cost
        mCost = data.byteArrayToInt(5, 4),
        mType = data[9].toInt(),
        mStation = data.byteArrayToLong(10, 6),
        mTime = data.byteArrayToLong(16, 7))
}

abstract class ChinaTripAbstract: Trip() {
    abstract val capsule : ChinaTripCapsule

    val mTime get() = capsule.mTime
    private val mCost get() = capsule.mCost
    val mStation get() = capsule.mStation
    val mType get() = capsule.mType

    override val fare: TransitCurrency?
        get() = TransitCurrency.CNY(if (isTopup) -mCost else mCost)

    protected val isTopup: Boolean
        get() = mType == 2

    protected val transport: Int
        get() = (mStation shr 28).toInt()

    val timestamp: Timestamp
        get() = ChinaTransitData.parseHexDateTime(mTime)

    val isValid: Boolean
        get() = mCost != 0 || mTime != 0L

    // Should be overridden if anything is known about transports
    override val mode: Mode
        get() = if (isTopup) Mode.TICKET_MACHINE else Mode.OTHER

    // Should be overridden if anything is known about transports
    override val routeName: FormattedString?
        get() = humanReadableRouteID?.let { FormattedString(it) }

    override val humanReadableRouteID: String?
        get() = mStation.toString(16) + "/" + mType

    override val startTimestamp: Timestamp?
        get() = timestamp
}

@Parcelize
class ChinaTrip (override val capsule: ChinaTripCapsule): ChinaTripAbstract() {
    constructor(data: ImmutableByteArray) : this(ChinaTripCapsule(data))
}
