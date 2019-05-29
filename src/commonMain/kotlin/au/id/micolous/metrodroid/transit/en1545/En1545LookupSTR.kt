/*
 * En1545LookupSTR.kt
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

package au.id.micolous.metrodroid.transit.en1545

import au.id.micolous.metrodroid.util.NumberUtils

import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.StationTableReader

abstract class En1545LookupSTR protected constructor(private val mStr: String) : En1545Lookup {

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == null)
            return null
        val routeId = routeNumber or ((agency ?: 0) shl 16) or ((transport ?: 0) shl 24)
        val routeReadable = getHumanReadableRouteId(routeNumber, routeVariant, agency ?: 0, transport ?: 0)
        return StationTableReader.getLineName(mStr, routeId, routeReadable!!)
    }

    override fun getAgencyName(agency: Int?, isShort: Boolean): String? {
        if (agency == null || agency == 0)
            return null
        return StationTableReader.getOperatorName(mStr, agency, isShort)
    }

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (station == 0)
            return null
        return StationTableReader.getStation(
                mStr,
                station or ((agency ?: 0) shl 16) or ((transport ?: 0) shl 24),
                NumberUtils.intToHex(station))
    }

    override fun getMode(agency: Int?, route: Int?): Trip.Mode {
        if (route != null) {
            val mode: Trip.Mode? =
                    if (agency == null)
                        StationTableReader.getLineMode(mStr, route)
                    else
                        StationTableReader.getLineMode(mStr, route or (agency shl 16))
            if (mode != null)
                return mode
        }
        if (agency == null)
            return Trip.Mode.OTHER
        return StationTableReader.getOperatorDefaultMode(mStr, agency)
    }
}
