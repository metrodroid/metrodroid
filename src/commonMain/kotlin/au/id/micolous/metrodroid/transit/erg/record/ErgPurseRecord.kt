/*
 * ErgPurseRecord.kt
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

package au.id.micolous.metrodroid.transit.erg.record

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Represents a "purse" type record.
 *
 * These are simple transactions where there is either a credit or debit from the purse value.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#purse-records
 */
@Parcelize
class ErgPurseRecord(val agency: Int,
                     val day: Int,
                     val minute: Int,
                     val isCredit: Boolean,
                     val transactionValue: Int,
                     val isTrip: Boolean) : ErgRecord(), Parcelable {
    override fun toString(): String {
        return "[ErgPurseRecord: agencyID=0x${agency.toString(16)}, day=$day, minute=$minute, isCredit=$isCredit, isTransfer=$isTrip, txnValue=$transactionValue]"
    }

    companion object {
        fun recordFromBytes(block: ImmutableByteArray): ErgRecord? {
            //if (input[0] != 0x02) throw new AssertionError("PurseRecord input[0] != 0x02");

            val isCredit: Boolean
            val isTrip: Boolean
            when (block[3].toInt()) {
                0x09, /* manly */ 0x0D /* chc */ -> {
                    isCredit = false
                    isTrip = false
                }
                0x08 /* chc, manly */ -> {
                    isCredit = true
                    isTrip = false
                }
                0x02 /* chc */ -> {
                    // For every non-paid trip, CHC puts in a 0x02
                    // For every paid trip, CHC puts a 0x0d (purse debit) and 0x02
                    isCredit = false
                    isTrip = true
                }
                else -> // May also be null or empty record...
                    return null
            }

            val record = ErgPurseRecord(
                    // Multiple agencyID IDs seen on chc cards -- might represent different operating companies.
                    agency = block.byteArrayToInt(1, 2),
                    day = block.getBitsFromBuffer(32, 20),
                    minute = block.getBitsFromBuffer(52, 12),
                    transactionValue = block.byteArrayToInt(8, 4),
                    isCredit = isCredit,
                    isTrip = isTrip
            )

            if (record.day < 0) throw AssertionError("Day < 0")
            if (record.minute > 1440)
                throw AssertionError("Minute > 1440 (${record.minute})")
            if (record.minute < 0)
                throw AssertionError("Minute < 0 (${record.minute})")

            //if (record.mTransactionValue < 0) throw new AssertionError("Value < 0");
            return record
        }
    }
}
