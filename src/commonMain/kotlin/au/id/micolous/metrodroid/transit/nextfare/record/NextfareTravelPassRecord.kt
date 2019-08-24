/*
 * NextfareTravelPassRecord.kt
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
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Travel pass record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */

class NextfareTravelPassRecord private constructor(
    private val mVersion: Int,
    val timestamp: Timestamp,
    val checksum: Int) : NextfareRecord, Comparable<NextfareTravelPassRecord> {
    override fun compareTo(other: NextfareTravelPassRecord) =
        // So sorting works, we reverse the order so highest number is first.
        other.mVersion.compareTo(this.mVersion)

    companion object {
        private const val TAG = "NextfareTravelPassRec"

        fun recordFromBytes(input: ImmutableByteArray, timeZone: MetroTimeZone): NextfareTravelPassRecord? {
            //if ((input[0] != 0x01 && input[0] != 0x31) || input[1] != 0x01) throw new AssertionError("Not a topup record");
            if (input.byteArrayToInt(2, 4) == 0) {
                // Timestamp is null, ignore.
                return null
            }

            val record = NextfareTravelPassRecord(
                    input.byteArrayToInt(13, 1),
                    NextfareRecord.unpackDate(input, 2, timeZone),
                    input.byteArrayToIntReversed(14, 2))


            Log.d(TAG, "@${record.timestamp}: version ${record.mVersion}")

            if (record.mVersion == 0) {
                // There is no travel pass loaded on to this card.
                return null
            }
            return record
        }
    }
}
