/*
 * NextfareConfigRecord.kt
 *
 * Copyright 2016-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.nextfare.record

import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString

/**
 * Represents a configuration record on Nextfare MFC.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */

@Parcelize
class NextfareConfigRecord (val ticketType: Int,
                            val expiry: Timestamp): NextfareRecord(), Parcelable {

    companion object {
        private const val TAG = "NextfareConfigRecord"

        fun recordFromBytes(input: ImmutableByteArray, timeZone: MetroTimeZone): NextfareConfigRecord {
            //if (input[0] != 0x01) throw new AssertionError();

            // Expiry date
            val record = NextfareConfigRecord(
                    expiry = unpackDate(input, 4, timeZone),

                    // Treat ticket type as little-endian
                    ticketType = input.byteArrayToIntReversed(8, 2)
            )

            Log.d(TAG, "Ticket type = ${record.ticketType.hexString}, expires ${record.expiry}")
            return record
        }
    }
}

