/*
 * BilheteUnicoSPTrip.kt
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
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip

@Parcelize
internal class BilheteUnicoSPTrip (private val mDay: Int,
                                   private val mTime: Int,
                                   private val mTransport: Int,
                                   private val mLocation: Int,
                                   private val mLine: Int,
                                   private val mFare: Int): Trip() {

    override val startTimestamp: Timestamp?
        get() = EPOCH.dayMinute(mDay, mTime)

    override val fare: TransitCurrency?
        get() = TransitCurrency.BRL(mFare)

    override val mode: Trip.Mode
        get() = when (mTransport) {
                BUS -> Trip.Mode.BUS
                TRAM -> Trip.Mode.TRAM
                else -> Trip.Mode.OTHER
            }

    override val routeName: FormattedString?
        get() = FormattedString(if (mTransport == BUS && mLine == 0x38222) mLocation.toString(16) else mLine.toString(16))

    override val humanReadableRouteID: String?
        get() = if (mTransport == BUS && mLine == 0x38222) mLocation.toString(16) else mLine.toString(16)

    override val startStation: Station?
        get() = if (mTransport == BUS && mLine == 0x38222) null else Station.unknown(mLocation)

    override fun getAgencyName(isShort: Boolean) = FormattedString(mTransport.toString(16))

    companion object {
        val EPOCH = Epoch.local(2000, MetroTimeZone.SAO_PAULO)
        private const val BUS = 0xb4
        private const val TRAM = 0x78

        fun parse(lastTripSector: ClassicSector): BilheteUnicoSPTrip {
            val block0 = lastTripSector.getBlock(0).data
            val block1 = lastTripSector.getBlock(1).data

            return BilheteUnicoSPTrip(
                    mTransport = block0.getBitsFromBuffer(0, 8),
                    mLocation = block0.getBitsFromBuffer(8, 20),
                    mLine = block0.getBitsFromBuffer(28, 20),
                    mFare = block1.getBitsFromBuffer(36, 16),
                    mDay = block1.getBitsFromBuffer(76, 14),
                    mTime = block1.getBitsFromBuffer(90, 11))
        }
    }
}
