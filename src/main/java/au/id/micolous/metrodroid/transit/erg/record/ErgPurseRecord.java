/*
 * ErgPurseRecord.java
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

package au.id.micolous.metrodroid.transit.erg.record;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.metrodroid.xml.ImmutableByteArray;

import java.util.Locale;

/**
 * Represents a "purse" type record.
 *
 * These are simple transactions where there is either a credit or debit from the purse value.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#purse-records
 */
public class ErgPurseRecord extends ErgRecord implements Parcelable {
    private int mRoute;
    private int mDay;
    private int mMinute;
    private boolean mIsCredit;
    private int mTransactionValue;
    private boolean mIsTrip;

    private ErgPurseRecord() {
    }

    public ErgPurseRecord(Parcel parcel) {
        mRoute = parcel.readInt();
        mDay = parcel.readInt();
        mMinute = parcel.readInt();
        mIsCredit = parcel.readInt() == 1;
        mTransactionValue = parcel.readInt();
        mIsTrip = parcel.readInt() == 1;
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

    public static ErgPurseRecord recordFromBytes(ImmutableByteArray input) {
        //if (input[0] != 0x02) throw new AssertionError("PurseRecord input[0] != 0x02");

        ErgPurseRecord record = new ErgPurseRecord();
        if (input.get(3) == 0x09 || /* manly */
                input.get(3) == 0x0D /* chc */) {
            record.mIsCredit = false;
            record.mIsTrip = false;
        } else if (input.get(3) == 0x08 /* chc, manly */) {
            record.mIsCredit = true;
            record.mIsTrip = false;
        } else if (input.get(3) == 0x02 /* chc */) {
            // For every non-paid trip, CHC puts in a 0x02
            // For every paid trip, CHC puts a 0x0d (purse debit) and 0x02
            record.mIsCredit = false;
            record.mIsTrip = true;
        } else {
            // May also be null or empty record...
            return null;
        }

        // Multiple agency IDs seen on chc cards -- might represent different operating companies.
        record.mRoute = input.byteArrayToInt(1, 2);

        record.mDay = input.getBitsFromBuffer(32, 20);
        if (record.mDay < 0) throw new AssertionError("Day < 0");

        record.mMinute = input.getBitsFromBuffer(52, 12);
        if (record.mMinute > 1440)
            throw new AssertionError(String.format(Locale.ENGLISH, "Minute > 1440 (%d)", record.mMinute));
        if (record.mMinute < 0)
            throw new AssertionError(String.format(Locale.ENGLISH, "Minute < 0 (%d)", record.mMinute));

        record.mTransactionValue = input.byteArrayToInt(8, 4);
        //if (record.mTransactionValue < 0) throw new AssertionError("Value < 0");
        return record;
    }

    public static final ErgRecord.Factory FACTORY = ErgPurseRecord::recordFromBytes;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mRoute);
        parcel.writeInt(mDay);
        parcel.writeInt(mMinute);
        parcel.writeInt(mIsCredit ? 1 : 0);
        parcel.writeInt(mTransactionValue);
        parcel.writeInt(mIsTrip ? 1 : 0);
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

    public boolean isCredit() {
        return mIsCredit;
    }

    public boolean isTrip() {
        return mIsTrip;
    }

    public int getRoute() {
        return mRoute;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "[%s: route=%x, day=%d, minute=%d, isCredit=%s, isTransfer=%s, txnValue=%d]",
                getClass().getSimpleName(),
                mRoute,
                mDay,
                mMinute,
                mIsCredit ? "true" : "false",
                mIsTrip ? "true" : "false",
                mTransactionValue);
    }
}