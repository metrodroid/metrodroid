/*
 * BilheteUnicoSPFirstTap.kt
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

package au.id.micolous.metrodroid.transit.bilhete_unico

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip

@Parcelize
internal class BilheteUnicoSPFirstTap (private val mDay: Int,
                                       private val mTime: Int,
                                       private val mLine: Int): Trip() {

    override val startTimestamp: Timestamp?
        get() = BilheteUnicoSPTrip.EPOCH.dayMinute(mDay, mTime)

    override val fare: TransitCurrency?
        get() = null

    override val mode: Mode
        get() {
            when (mLine shr 5) {
                1 -> return Mode.BUS
                2 -> return Mode.TRAM
            }
            return Mode.OTHER
        }

    override val routeName: FormattedString?
        get() = FormattedString(mLine.toString(16))

    override val humanReadableRouteID: String?
        get() = mLine.toString(16)
}
