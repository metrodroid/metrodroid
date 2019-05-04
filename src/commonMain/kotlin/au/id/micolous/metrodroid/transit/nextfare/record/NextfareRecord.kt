/*
 * NextfareRecord.kt
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
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * Represents a record on a Nextfare card
 * This fans out parsing to subclasses.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
open class NextfareRecord protected constructor() {
    companion object {
        private const val TAG = "NextfareRecord"
        fun recordFromBytes(input: ImmutableByteArray, sectorIndex: Int, blockIndex: Int, timeZone: MetroTimeZone): NextfareRecord? {
            Log.d(TAG, "Record: $input")

            when {
                sectorIndex == 1 && blockIndex <= 1 -> {
                    Log.d(TAG, "Balance record")
                    return NextfareBalanceRecord.recordFromBytes(input)
                }
                sectorIndex == 1 && blockIndex == 2 -> {
                    Log.d(TAG, "Configuration record")
                    return NextfareConfigRecord.recordFromBytes(input, timeZone)
                }
                sectorIndex == 2 -> {
                    Log.d(TAG, "Top-up record")
                    return NextfareTopupRecord.recordFromBytes(input, timeZone)
                }
                sectorIndex == 3 -> {
                    Log.d(TAG, "Travel pass record")
                    return NextfareTravelPassRecord.recordFromBytes(input, timeZone)
                }
                sectorIndex in 5..8 -> {
                    Log.d(TAG, "Transaction record")
                    return NextfareTransactionRecord.recordFromBytes(input, timeZone)
                }
                else -> return null
            }
        }

        /**
         * Date format:
         *
         *
         * Top two bytes:
         * 0001111 1100 00100 = 2015-12-04
         * yyyyyyy mmmm ddddd
         *
         *
         * Bottom 11 bits = minutes since 00:00
         * Time is represented in localtime
         *
         *
         * Assumes that data has not been byte-reversed.
         *
         * @param input Bytes of input representing the timestamp to parse
         * @param offset Offset in byte to timestamp
         * @return Date and time represented by that value
         */
        fun unpackDate(input: ImmutableByteArray, offset: Int, timeZone: MetroTimeZone): TimestampFull {
            val timestamp = input.byteArrayToIntReversed(offset, 4)
            val minute = NumberUtils.getBitsFromInteger(timestamp, 16, 11)
            val year = NumberUtils.getBitsFromInteger(timestamp, 9, 7) + 2000
            val month = NumberUtils.getBitsFromInteger(timestamp, 5, 4)
            val day = NumberUtils.getBitsFromInteger(timestamp, 0, 5)

            //noinspection MagicCharacter
            Log.d(TAG, "unpackDate: $minute minutes, $year-$month-$day")

            if (minute > 1440)
                throw AssertionError("Minute > 1440 ($minute)")
            if (minute < 0)
                throw AssertionError("Minute < 0 ($minute)")

            if (day > 31) throw AssertionError("Day > 31 ($day)")
            if (month > 12)
                throw AssertionError("Month > 12 ($month)")

            return TimestampFull(timeZone, year, month - 1, day, minute / 60, minute % 60, 0)
        }
    }
}
