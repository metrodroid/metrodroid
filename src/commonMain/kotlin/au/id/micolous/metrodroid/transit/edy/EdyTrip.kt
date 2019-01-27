/*
 * EdyTransitData.java
 *
 * Copyright 2013 Chris Norden
 * Copyright 2013-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
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
package au.id.micolous.metrodroid.transit.edy

import au.id.micolous.metrodroid.card.felica.FelicaBlock
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class EdyTrip (private val mProcessType: Int,
               private val mSequenceNumber: Int,
               override val startTimestamp: Timestamp?,
               private val mTransactionAmount: Int,
               private val mBalance: Int): Trip() {

    // TODO: Revisit this, and check that these Modes are sensible.
    override val mode: Trip.Mode
        get() = when (mProcessType) {
            EdyTransitData.FELICA_MODE_EDY_DEBIT -> Trip.Mode.POS
            EdyTransitData.FELICA_MODE_EDY_CHARGE -> Trip.Mode.TICKET_MACHINE
            EdyTransitData.FELICA_MODE_EDY_GIFT -> Trip.Mode.VENDING_MACHINE
            else -> Trip.Mode.OTHER
        }

    // Credits are "negative"
    override val fare: TransitCurrency?
        get() = if (mProcessType != EdyTransitData.FELICA_MODE_EDY_DEBIT) {
            TransitCurrency.JPY(-mTransactionAmount)
        } else TransitCurrency.JPY(mTransactionAmount)

    companion object {

        fun parse(block: FelicaBlock): EdyTrip {
            val data = block.data

            // Data Offsets with values
            // ------------------------
            // 0x00    type (0x20 = payment, 0x02 = charge, 0x04 = gift)
            // 0x01    sequence number (3 bytes, big-endian)
            // 0x04    date/time (upper 15 bits - added as day offset, lower 17 bits - added as second offset to Jan 1, 2000 00:00:00)
            // 0x08    transaction amount (big-endian)
            // 0x0c    balance (big-endian)

            return EdyTrip(
                    mProcessType = data[0].toInt(),
                    mSequenceNumber = data.byteArrayToInt(1, 3),
                    startTimestamp = extractDate(data),
                    mTransactionAmount = data.byteArrayToInt(8, 4),
                    mBalance = data.byteArrayToInt(12, 4)
            )
        }

        private fun extractDate(data: ImmutableByteArray): Timestamp? {
            val fulloffset = data.byteArrayToInt(4, 4)
            if (fulloffset == 0)
                return null

            val dateoffset = fulloffset.ushr(17)
            val timeoffset = fulloffset and 0x1ffff

            return EdyTransitData.EPOCH.daySecond(dateoffset, timeoffset)
        }
    }
}
