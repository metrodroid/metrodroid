/*
 * ManlyFastFerryPurseRecord.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
class ErgPurseRecord(private val mAgency: Int = 0,
                     val day: Int = 0,
                     val minute: Int = 0,
                     val isCredit: Boolean = false,
                     val transactionValue: Int = 0) : ErgRecord(), Parcelable {

    override fun toString() =
            "[ErgPurseRecord: agency=$mAgency, day=$day, minute=$minute, isCredit=$isCredit, txnValue=$transactionValue]"

    companion object {

        fun recordFromBytes(input: ImmutableByteArray): ErgPurseRecord? {
            //if (input[0] != 0x02) throw new AssertionError("PurseRecord input[0] != 0x02");

            val isCredit = when (input[3].toInt()) {
                0x09, /* manly */ 0x0D /* chc */ -> false
                0x08 /* chc, manly */ -> true
                else -> // chc: 0x02 seen here, but only on $0 trips. Suspect this is for 2-hour free transfers.
                    // Not really important for MD, nor does it fit neatly into the data model.

                    // May also be null or empty record...
                    return null
            }

            // Multiple agency IDs seen on chc cards -- might represent different operating companies.
            val mAgency = input.byteArrayToInt(1, 2)

            val day = input.getBitsFromBuffer(32, 20)
            if (day < 0) throw AssertionError("Day < 0")

            val minute = input.getBitsFromBuffer(52, 12)
            if (minute > 1440)
                throw AssertionError("Minute > 1440 ($minute)")
            if (minute < 0)
                throw AssertionError("Minute < 0 ($minute)")

            val transactionValue = input.byteArrayToInt(8, 4)
            //if (record.mTransactionValue < 0) throw new AssertionError("Value < 0");
            return ErgPurseRecord(isCredit = isCredit, mAgency = mAgency, day = day, minute = minute,
                    transactionValue = transactionValue)
        }
    }
}
