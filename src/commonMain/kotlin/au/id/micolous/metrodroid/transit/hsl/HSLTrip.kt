/*
 * HSLTrip.kt
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.hsl

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.multi.R

import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class HSLTrip(internal var mLine: String? = null,
              internal var mVehicleNumber: Int = 0,
              override val startTimestamp: TimestampFull? = null,
              internal val mFare: Int = 0,
              internal val mArvo: Int = 0,
              internal val expireTimestamp: TimestampFull? = null,
              override val passengerCount: Int = 0) : Trip() {

    override val routeName: String?
        get() = if (mLine != null) {
            mLine!!.substring(1)
        } else null

    override val humanReadableRouteID: String?
        get() = null

    override val vehicleID: String?
        get() = if (mVehicleNumber != 0 && mVehicleNumber != -1) mVehicleNumber.toString() else null

    override val fare: TransitCurrency?
        get() = TransitCurrency.EUR(mFare)

    override val mode: Trip.Mode
        get() {
            if (mLine == null)
                return Trip.Mode.BUS

            if (mLine == "1300")
                return Trip.Mode.METRO
            if (mLine == "1019")
                return Trip.Mode.FERRY
            if (mLine!!.startsWith("100") || mLine == "1010")
                return Trip.Mode.TRAM
            return if (mLine!!.startsWith("3")) Trip.Mode.TRAIN else Trip.Mode.BUS
        }

    constructor(useData: ImmutableByteArray) : this(
        mArvo = useData.getBitsFromBuffer(0, 1),

        startTimestamp = HSLTransitData.cardDateToCalendar(
                useData.getBitsFromBuffer(1, 14),
                useData.getBitsFromBuffer(15, 11)),
        expireTimestamp = HSLTransitData.cardDateToCalendar(
                useData.getBitsFromBuffer(26, 14),
                useData.getBitsFromBuffer(40, 11)),

        mFare = useData.getBitsFromBuffer(51, 14),

        passengerCount = useData.getBitsFromBuffer(65, 5),
        mLine = null,
        mVehicleNumber = -1/*,

        mNewBalance = useData.getBitsFromBuffer(70, 20)*/)

    override fun getAgencyName(isShort: Boolean): String? {
        if (mArvo == 1) {
            val mins = Localizer.localizeString(R.string.hsl_mins_format, ((this.expireTimestamp!!.timeInMillis - this.startTimestamp!!.timeInMillis) / 60000L).toString())
            val type = Localizer.localizeString(R.string.hsl_balance_ticket)
            return "$type, $mins"
        } else {
            return Localizer.localizeString(R.string.hsl_pass_ticket)
        }
    }
}
