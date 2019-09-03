/*
 * CompassUltralightTrip.kt
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

package au.id.micolous.metrodroid.transit.yvr_compass

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.nextfareul.NextfareUltralightTransaction
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.StationTableReader

@Suppress("CanBeParameter")
@Parcelize
class CompassUltralightTransaction (val rawData: ImmutableByteArray,
                                    val baseDate: Int): NextfareUltralightTransaction(rawData, baseDate) {

    override val station: Station?
        get() = if (mLocation == 0) null else StationTableReader.getStation(COMPASS_STR, mLocation)

    override val timezone: MetroTimeZone
        get() = CompassUltralightTransitData.TZ

    override val isBus: Boolean
        get() = mRoute == 5 || mRoute == 7

    override val mode: Trip.Mode
        get() {
            if (isBus)
                return Trip.Mode.BUS
            if (mRoute == 3 || mRoute == 9 || mRoute == 0xa)
                return Trip.Mode.TRAIN
            return if (mRoute == 0) Trip.Mode.TICKET_MACHINE else Trip.Mode.OTHER
        }

    companion object {
        private const val COMPASS_STR = "compass"
    }
}
