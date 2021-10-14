/*
 * NextfareTrip.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.nextfare

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.XXX
import au.id.micolous.metrodroid.transit.TransitCurrencyRef
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTopupRecord
import au.id.micolous.metrodroid.util.StationTableReader

@Parcelize
class NextfareTripCapsule(internal var mJourneyId: Int = 0,
                          internal var isTopup: Boolean = false,
                          internal var mModeInt: Int = 0,
                          internal var startTimestamp: TimestampFull? = null,
                          internal var endTimestamp: TimestampFull? = null,
                          internal var mStartStation: Int = -1,
                          internal var mEndStation: Int = -1,
                          internal var isTransfer: Boolean = false,
                          internal var mCost: Int = 0): Parcelable {
    constructor(rec: NextfareTopupRecord) : this(
        startTimestamp = rec.timestamp, isTopup = true,
            mCost = rec.credit * -1)
}

/**
 * Represents trips on Nextfare
 */
abstract class NextfareTrip : Trip(), Comparable<NextfareTrip> {
    abstract val capsule: NextfareTripCapsule
    abstract val currency: TransitCurrencyRef
    abstract val str: String?

    override val isTransfer get() = capsule.isTransfer

    override val startTimestamp get() = capsule.startTimestamp

    override val endTimestamp get() = capsule.endTimestamp

    override val startStation: Station?
        get() = if (capsule.mStartStation < 0) {
            null
        } else getStation(capsule.mStartStation)

    override val endStation: Station?
        get() = if (capsule.endTimestamp != null && capsule.mEndStation > -1) {
            getStation(capsule.mEndStation)
        } else {
            null
        }

    override val fare: TransitCurrency?
        get() = currency(capsule.mCost)

    override val mode: Mode
        get() = if (capsule.isTopup) Mode.TICKET_MACHINE else lookupMode()

    protected open fun getStation(stationId: Int): Station? {
        return StationTableReader.getStation(str, stationId)
    }

    protected open fun lookupMode(): Mode {
        return StationTableReader.getOperatorDefaultMode(str, capsule.mModeInt)
    }

    override fun compareTo(other: NextfareTrip): Int {
        return this.startTimestamp!!.compareTo(other.startTimestamp!!)
    }

    override fun getAgencyName(isShort: Boolean) =
        if (capsule.isTopup && capsule.mModeInt == 0)
            null
        else
            StationTableReader.getOperatorName(str, capsule.mModeInt, isShort)
}

@Parcelize
class NextfareUnknownTrip (override val capsule: NextfareTripCapsule): NextfareTrip() {
    override val currency: TransitCurrencyRef
        get() = ::XXX
    override val str: String?
        get() = null
}
