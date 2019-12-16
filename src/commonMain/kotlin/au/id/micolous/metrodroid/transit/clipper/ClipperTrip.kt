/*
 * ClipperTrip.kt
 *
 * Copyright 2011 "an anonymous contributor"
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class ClipperTrip (private val mTimestamp: Long,
                   private val mExitTimestamp: Long,
                   private val mFare: Int,
                   private val mAgency: Int,
                   private val mFrom: Int,
                   private val mTo: Int,
                   private val mRoute: Int,
                   private val mVehicleNum: Int,
                   private val mTransportCode: Int): Trip() {

    override val startTimestamp: Timestamp?
        get() = ClipperTransitData.clipperTimestampToCalendar(mTimestamp)

    override val endTimestamp: Timestamp?
        get() = ClipperTransitData.clipperTimestampToCalendar(mExitTimestamp)

    // Bus doesn't record line
    override val routeName: FormattedString?
        get() = if (mAgency == ClipperData.AGENCY_GG_FERRY) {
            ClipperData.GG_FERRY_ROUTES[mRoute]
        } else {
            null
        }

    override val humanReadableRouteID: String?
        get() = if (mAgency == ClipperData.AGENCY_GG_FERRY) {
            NumberUtils.intToHex(mRoute)
        } else null

    override val vehicleID: String?
        get() = when (mVehicleNum) {
            0, 0xffff -> null
            in 1..9999 -> mVehicleNum.toString()
            // For LRV4 Muni vehicles with newer Clipper readers, it stores a 4-digit vehicle number followed
            // by a letter. For example 0d20461 is vehicle number 2046A, 0d20462 is 2046B, and so on.
            else -> (mVehicleNum / 10).toString() + Integer.toHexString((mVehicleNum % 10) + 9).toString().toUpperCase()
        }

    override val fare: TransitCurrency?
        get() = TransitCurrency.USD(mFare)

    override val startStation: Station?
        get() = ClipperData.getStation(mAgency, mFrom, false)

    override val endStation: Station?
        get() = ClipperData.getStation(mAgency, mTo, true)

    override val mode: Trip.Mode
        get() = when (mTransportCode) {
            0x62 -> {
                when (mAgency) {
                    ClipperData.AGENCY_BAY_FERRY, ClipperData.AGENCY_GG_FERRY -> Trip.Mode.FERRY
                    ClipperData.AGENCY_CALTRAIN -> Trip.Mode.TRAIN
                    else -> Trip.Mode.TRAM
                }
            }
            0x6f -> Trip.Mode.METRO
            0x61, 0x75 -> Trip.Mode.BUS
            else -> Trip.Mode.OTHER
        }

    internal constructor(useData: ImmutableByteArray): this(
        mAgency = useData.byteArrayToInt(0x2, 2),
        mFare = useData.byteArrayToInt(0x6, 2),
        mVehicleNum = useData.byteArrayToInt(0xa, 2),
        mTimestamp = useData.byteArrayToLong(0xc, 4),
        mExitTimestamp = useData.byteArrayToLong(0x10, 4),
        mFrom = useData.byteArrayToInt(0x14, 2),
        mTo = useData.byteArrayToInt(0x16, 2),
        mRoute = useData.byteArrayToInt(0x1c, 2),
        mTransportCode = useData.byteArrayToInt(0x1e, 2)
    )

    override fun getAgencyName(isShort: Boolean) = ClipperData.getAgencyName(mAgency, isShort)
}
