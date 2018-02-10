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

package au.id.micolous.metrodroid.transit.erg.record;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.metrodroid.util.Utils;

import java.util.Locale;

/**
 * Represents a "purse" type record.
 *
 * These are simple transactions where there is either a credit or debit from the purse value.
 */
public class ErgPurseRecord extends ErgRecord implements Parcelable {
    private int mDay;
    private int mMinute;
    private boolean mIsCredit;
    private int mTransactionValue;

    protected ErgPurseRecord() {
    }

    public ErgPurseRecord(Parcel parcel) {
        mDay = parcel.readInt();
        mMinute = parcel.readInt();
        mIsCredit = parcel.readInt() == 1;
        mTransactionValue = parcel.readInt();
    }

    public static final Creator<ErgPurseRecord> CREATOR = new Creator<ErgPurseRecord>() {
        @Override
        public ErgPurseRecord createFromParcel(Parcel in) {
            return new ErgPurseRecord(in);
        }

        @Override
        public ErgPurseRecord[] newArray(int size) {
            return new ErgPurseRecord[size];
        }
    };

    public static ErgPurseRecord recordFromBytes(byte[] input) {
        //if (input[0] != 0x02) throw new AssertionError("PurseRecord input[0] != 0x02");

        ErgPurseRecord record = new ErgPurseRecord();
        if (input[3] == 0x09) {
            record.mIsCredit = false;
        } else if (input[3] == 0x08) {
            record.mIsCredit = true;
        } else {
            // bad record?
            return null;
        }

        record.mDay = Utils.getBitsFromBuffer(input, 32, 20);
        if (record.mDay < 0) throw new AssertionError("Day < 0");

        record.mMinute = Utils.getBitsFromBuffer(input, 52, 12);
        if (record.mMinute > 1440)
            throw new AssertionError(String.format(Locale.ENGLISH, "Minute > 1440 (%d)", record.mMinute));
        if (record.mMinute < 0)
            throw new AssertionError(String.format(Locale.ENGLISH, "Minute < 0 (%d)", record.mMinute));

        record.mTransactionValue = Utils.byteArrayToInt(input, 8, 4);
        //if (record.mTransactionValue < 0) throw new AssertionError("Value < 0");

        return record;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mDay);
        parcel.writeInt(mMinute);
        parcel.writeInt(mIsCredit ? 1 : 0);
        parcel.writeInt(mTransactionValue);
    }

    public int getDay() {
        return mDay;
    }

    public int getMinute() {
        return mMinute;
    }

    public int getTransactionValue() {
        return mTransactionValue;
    }

    public boolean getIsCredit() {
        return mIsCredit;
    }

}
