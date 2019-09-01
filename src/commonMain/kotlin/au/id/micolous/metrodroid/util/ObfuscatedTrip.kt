/*
 * ObfuscatedTrip.kt
 *
 * Copyright 2017 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip

/**
 * Special wrapper for Trip that handles obfuscation of Trip data.
 */
@Parcelize
internal class ObfuscatedTrip (
        override val startTimestamp: Timestamp?,
        override val endTimestamp: Timestamp?,
        override val routeName: FormattedString?,
        override val startStation: Station?,
        override val mode: Trip.Mode,
        override val endStation: Station?,
        override val fare: TransitCurrency?,
        override val humanReadableRouteID: String?,
        override val vehicleID: String?,
        override val machineID: String?,
        override val passengerCount: Int,
        private val mAgencyName: FormattedString?,
        private val mShortAgencyName: FormattedString?
): Trip() {

    constructor(realTrip: Trip, timeDelta: Long, obfuscateFares: Boolean) : this (
            routeName = realTrip.routeName,
            mAgencyName = realTrip.getAgencyName(false),
            mShortAgencyName = realTrip.getAgencyName(true),
            startStation = realTrip.startStation,
            endStation = realTrip.endStation,
            mode = realTrip.mode,
            passengerCount = realTrip.passengerCount,
            vehicleID = realTrip.vehicleID,
            machineID = realTrip.machineID,
            humanReadableRouteID = realTrip.humanReadableRouteID,
            fare = realTrip.fare?.let {
                if (obfuscateFares)
                    it.obfuscate()
                else
                    it
            },
            startTimestamp = realTrip.startTimestamp?.obfuscateDelta(timeDelta),
            endTimestamp = realTrip.endTimestamp?.obfuscateDelta(timeDelta)
    )

    override fun getAgencyName(isShort: Boolean) =
        if (isShort) mShortAgencyName else mAgencyName
}
