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

import android.os.Parcel
import android.os.Parcelable

import au.id.micolous.metrodroid.xml.ImmutableByteArray

import java.util.Locale

/**
 * Represents a "purse" type record.
 *
 * These are simple transactions where there is either a credit or debit from the purse value.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#purse-records
 */
class ErgPurseRecord : ErgRecord, Parcelable {
    var agency: Int = 0
        private set
    var day: Int = 0
        private set
    var minute: Int = 0
        private set
    var isCredit: Boolean = false
        private set
    var transactionValue: Int = 0
        private set
    var isTrip: Boolean = false
        private set

    private constructor() {}

    constructor(parcel: Parcel) {
        agency = parcel.readInt()
        day = parcel.readInt()
        minute = parcel.readInt()
        isCredit = parcel.readInt() == 1
        transactionValue = parcel.readInt()
        isTrip = parcel.readInt() == 1
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(agency)
        parcel.writeInt(day)
        parcel.writeInt(minute)
        parcel.writeInt(if (isCredit) 1 else 0)
        parcel.writeInt(transactionValue)
        parcel.writeInt(if (isTrip) 1 else 0)
    }

    override fun toString(): String {
        return String.format(Locale.ENGLISH, "[%s: agencyID=%x, day=%d, minute=%d, isCredit=%s, isTransfer=%s, txnValue=%d]",
                javaClass.simpleName,
                agency,
                day,
                minute,
                if (isCredit) "true" else "false",
                if (isTrip) "true" else "false",
                transactionValue)
    }

    companion object {
        val FACTORY: ErgRecord.Factory = object : ErgRecord.Factory() {
            override fun recordFromBytes(block: ImmutableByteArray): ErgRecord? {
                //if (input[0] != 0x02) throw new AssertionError("PurseRecord input[0] != 0x02");

                val record = ErgPurseRecord()
                if (block[3].toInt() == 0x09 || /* manly */ block[3].toInt() == 0x0D /* chc */) {
                    record.isCredit = false
                    record.isTrip = false
                } else if (block[3].toInt() == 0x08 /* chc, manly */) {
                    record.isCredit = true
                    record.isTrip = false
                } else if (block[3].toInt() == 0x02 /* chc */) {
                    // For every non-paid trip, CHC puts in a 0x02
                    // For every paid trip, CHC puts a 0x0d (purse debit) and 0x02
                    record.isCredit = false
                    record.isTrip = true
                } else {
                    // May also be null or empty record...
                    return null
                }

                // Multiple agencyID IDs seen on chc cards -- might represent different operating companies.
                record.agency = block.byteArrayToInt(1, 2)

                record.day = block.getBitsFromBuffer(32, 20)
                if (record.day < 0) throw AssertionError("Day < 0")

                record.minute = block.getBitsFromBuffer(52, 12)
                if (record.minute > 1440)
                    throw AssertionError(String.format(Locale.ENGLISH, "Minute > 1440 (%d)", record.minute))
                if (record.minute < 0)
                    throw AssertionError(String.format(Locale.ENGLISH, "Minute < 0 (%d)", record.minute))

                record.transactionValue = block.byteArrayToInt(8, 4)
                //if (record.mTransactionValue < 0) throw new AssertionError("Value < 0");
                return record
            }
        }

        @JvmField
        val CREATOR: Parcelable.Creator<ErgPurseRecord> = object : Parcelable.Creator<ErgPurseRecord> {
            override fun createFromParcel(parcel: Parcel): ErgPurseRecord {
                return ErgPurseRecord(parcel)
            }

            override fun newArray(size: Int): Array<ErgPurseRecord?> {
                return arrayOfNulls(size)
            }
        }
    }
}
