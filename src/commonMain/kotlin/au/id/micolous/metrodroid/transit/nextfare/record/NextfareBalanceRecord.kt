/*
 * NextfareBalanceRecord.kt
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
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Represents balance records on Nextfare
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
class NextfareBalanceRecord private constructor(
        val version: Int,
        val balance: Int,
        val hasTravelPassAvailable: Boolean) : NextfareRecord, Comparable<NextfareBalanceRecord> {

    override fun compareTo(other: NextfareBalanceRecord) =
        // So sorting works, we reverse the order so highest number is first.
        other.version.compareTo(this.version)

    companion object {
        private const val TAG = "NextfareBalanceRecord"

        fun recordFromBytes(input: ImmutableByteArray): NextfareBalanceRecord {
            //if (input[0] != 0x01) throw new AssertionError();

            val version = input.byteArrayToInt(13, 1)

            // Do some flipping for the balance
            var balance = input.byteArrayToIntReversed(2, 2)

            // Negative balance
            if (balance and 0x8000 == 0x8000) {
                // TODO: document which nextfares use a sign flag like this.
                balance = balance and 0x7fff
                balance *= -1
            } else if (input[1].toInt() and 0x80 == 0x80) {
                // seq_go uses a sign flag in an adjacent byte
                balance *= -1
            }

            val record = NextfareBalanceRecord(
                    version = version,
                    balance = balance,
                    hasTravelPassAvailable = (input[7].toInt() != 0x00)
            )

            Log.d(TAG, "Balance ${record.balance}, version ${record.version}, travel pass ${record.hasTravelPassAvailable}")
            return record
        }
    }
}
