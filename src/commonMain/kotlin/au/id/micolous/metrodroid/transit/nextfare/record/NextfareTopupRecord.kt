/*
 * NextfareTopupRecord.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Top-up record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
@Parcelize
class NextfareTopupRecord (
        val timestamp: TimestampFull?,
        val credit: Int,
        val station: Int,
        val checksum: Int,
        val isAutomatic: Boolean
): NextfareRecord, Parcelable {
    companion object {
        private const val TAG = "NextfareTopupRecord"

        fun recordFromBytes(input: ImmutableByteArray, timeZone: MetroTimeZone): NextfareTopupRecord? {
            //if ((input[0] != 0x01 && input[0] != 0x31) || input[1] != 0x01) throw new AssertionError("Not a topup record");

            // Check if all the other data is null
            if (input.byteArrayToLong(2, 6) == 0L) {
                Log.d(TAG, "Null top-up record, skipping")
                return null
            }

            val record = NextfareTopupRecord(
                    timestamp = NextfareRecord.unpackDate(input, 2, timeZone),
                    credit = input.byteArrayToIntReversed(6, 2) and 0x7FFF,
                    station = input.byteArrayToIntReversed(12, 2),
                    checksum = input.byteArrayToIntReversed(14, 2),
                    isAutomatic = input[0].toInt() == 0x31)

            Log.d(TAG, "@${record.timestamp}: ${record.credit} cents, station ${record.station}, isAuto ${record.isAutomatic}")
            return record
        }
    }
}
