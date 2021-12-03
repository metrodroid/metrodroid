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
import au.id.micolous.metrodroid.transit.TransitCurrencyBase
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.Trip

/**
 * Special wrapper for Trip that handles obfuscation of Trip data.
 */
@Parcelize
data class ObfuscatedTrip (
    override val startTimestamp: Timestamp? = null,
    override val endTimestamp: Timestamp? = null,
    override val routeName: FormattedString? = null,
    override val startStation: Station? = null,
    override val mode: Mode,
    override val endStation: Station? = null,
    override val fare: TransitCurrencyBase? = null,
    override val humanReadableRouteID: String? = null,
    override val vehicleID: String? = null,
    override val machineID: String? = null,
    override val passengerCount: Int,
    private val mAgencyName: FormattedString? = null,
    private val mShortAgencyName: FormattedString? = null,
    private val rawFieldsUnknown: String? = null,
    private val rawFieldsFull: String? = null,
): Trip() {

    override fun getRawFields(level: TransitData.RawLevel): String? = when {
        Preferences.hideCardNumbers || Preferences.obfuscateBalance || Preferences.obfuscateTripDates
                || Preferences.obfuscateTripFares || Preferences.obfuscateTripTimes -> null
        level == TransitData.RawLevel.ALL -> rawFieldsFull
        level == TransitData.RawLevel.UNKNOWN_ONLY -> rawFieldsUnknown
        else -> null
    }

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
            endTimestamp = realTrip.endTimestamp?.obfuscateDelta(timeDelta),
            rawFieldsFull = realTrip.getRawFields(TransitData.RawLevel.ALL),
            rawFieldsUnknown = realTrip.getRawFields(TransitData.RawLevel.UNKNOWN_ONLY)
    )

    override fun getAgencyName(isShort: Boolean) =
        if (isShort) mShortAgencyName else mAgencyName
}
