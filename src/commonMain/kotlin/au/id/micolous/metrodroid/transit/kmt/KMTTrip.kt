/*
 * KMTTrip.kt
 *
 * Copyright 2018 Bondan Sumbodo <sybond@gmail.com>
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

package au.id.micolous.metrodroid.transit.kmt

import au.id.micolous.metrodroid.card.felica.FelicaBlock
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class KMTTrip (private val mProcessType: Int,
               private val mSequenceNumber: Int,
               override val startTimestamp: Timestamp?,
               private val mTransactionAmount: Int,
               private val mEndGateCode: Int): Trip() {

    // Normally, only the end station is recorded.  But top-ups only have a "starting" station.
    override val startStation: Station?
        get() = if (mProcessType == 0 || mProcessType == 2) {
            getStation(mEndGateCode)
        } else null

    // "Ending station" doesn't make sense for Ticket Machines or Point-of-sale
    override val endStation: Station?
        get() = if (mProcessType == 0 || mProcessType == 2) {
            null
        } else getStation(mEndGateCode)

    override val mode: Trip.Mode
        get() = when (mProcessType) {
            0 -> Trip.Mode.TICKET_MACHINE
            1 -> Trip.Mode.TRAIN
            2 -> Trip.Mode.POS
            else -> Trip.Mode.OTHER
        }

    override val fare: TransitCurrency?
        get() = if (mProcessType != 1) {
            TransitCurrency.IDR(mTransactionAmount).negate()
        } else TransitCurrency.IDR(mTransactionAmount)

    override fun getAgencyName(isShort: Boolean) = Localizer.localizeString(R.string.kmt_agency)

    companion object {
        fun parse(block: FelicaBlock): KMTTrip {
            val data = block.data
            return KMTTrip(
                    mProcessType = data[12].toInt() and 0xff,
                    mSequenceNumber = data.byteArrayToInt(13, 3),
                    startTimestamp = calcDate(data),
                    mTransactionAmount = data.byteArrayToInt(4, 4),
                    mEndGateCode = data.byteArrayToInt(8, 2))
        }

        private const val KMT_STR = "kmt"

        private fun calcDate(data: ImmutableByteArray): Timestamp? {
            val fulloffset = data.byteArrayToInt(0, 4)
            if (fulloffset == 0) {
                return null
            }
            return KMTTransitData.KMT_EPOCH.seconds(fulloffset.toLong())
        }

        private fun getStation(code: Int) = StationTableReader.getStation(KMT_STR, code)
    }
}
