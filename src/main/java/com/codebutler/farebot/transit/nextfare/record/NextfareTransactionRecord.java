/*
 * NextfareTapRecord.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.nextfare.record;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.codebutler.farebot.transit.nextfare.NextfareUtil;
import com.codebutler.farebot.util.Utils;

import java.util.GregorianCalendar;

/**
 * Tap record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
public class NextfareTransactionRecord extends NextfareRecord implements Parcelable, Comparable<NextfareTransactionRecord> {
    private static final String TAG = "NextfareTxnRecord";
    private GregorianCalendar mTimestamp;
    private int mMode;
    private int mJourney;
    private int mStation;
    private int mValue;
    private int mChecksum;
    private boolean mContinuation;


    public static final Creator<NextfareTransactionRecord> CREATOR = new Creator<NextfareTransactionRecord>() {
        @Override
        public NextfareTransactionRecord createFromParcel(Parcel in) {
            return new NextfareTransactionRecord(in);
        }

        @Override
        public NextfareTransactionRecord[] newArray(int size) {
            return new NextfareTransactionRecord[size];
        }
    };

    public static NextfareTransactionRecord recordFromBytes(byte[] input) {
        //if (input[0] != 0x31) throw new AssertionError("not a tap record");

        // LAX:      input[0] == 0x05 for "Travel Pass" trips.
        // SEQ, LAX: input[0] == 0x31 for "Stored Value" trips / transfers
        // LAX:      input[0] == 0x41 for "Travel Pass" sale.
        // LAX:      input[0] == 0x71 for "Stored Value" sale -- effectively recorded twice
        // SEQ, LAX: input[0] == 0x79 for "Stored Value" sale

        if (input[0] > 0x70) {
            return null;
        }

        NextfareTransactionRecord record = new NextfareTransactionRecord();

        record.mMode = Utils.byteArrayToInt(input, 1, 1);

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mTimestamp = NextfareUtil.unpackDate(ts);

        byte[] journey = Utils.reverseBuffer(input, 5, 2);
        record.mJourney = Utils.byteArrayToInt(journey) >> 5;
        record.mContinuation = (Utils.byteArrayToInt(journey) & 0x10) > 1;

        byte[] value = Utils.reverseBuffer(input, 7, 2);
        record.mValue = Utils.byteArrayToInt(value);
        if (record.mValue > 0x8000) {
            record.mValue = -(record.mValue & 0x7fff);
        }

        byte[] station = Utils.reverseBuffer(input, 12, 2);
        record.mStation = Utils.byteArrayToInt(station);

        byte[] checksum = Utils.reverseBuffer(input, 14, 2);
        record.mChecksum = Utils.byteArrayToInt(checksum);

        Log.d(TAG, "@" + Utils.isoDateTimeFormat(record.mTimestamp) + ": mode " + record.mMode + ", station " + record.mStation + ", value " + record.mValue + ", journey " + record.mJourney + ", " + (record.mContinuation ? "continuation" : "new trip"));
        return record;
    }

    protected NextfareTransactionRecord() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mTimestamp.getTimeInMillis());
        parcel.writeInt(mMode);
        parcel.writeInt(mJourney);
        parcel.writeInt(mStation);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mContinuation ? 1 : 0);
        parcel.writeInt(mValue);
    }

    public NextfareTransactionRecord(Parcel parcel) {
        mTimestamp = new GregorianCalendar();
        mTimestamp.setTimeInMillis(parcel.readLong());
        mMode = parcel.readInt();
        mJourney = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
        mContinuation = parcel.readInt() == 1;
        mValue = parcel.readInt();
    }

    public int getMode() {
        return mMode;
    }

    public GregorianCalendar getTimestamp() {
        return mTimestamp;
    }

    public int getJourney() {
        return mJourney;
    }

    public int getStation() {
        return mStation;
    }

    public int getChecksum() {
        return mChecksum;
    }

    public boolean isContinuation() {
        return mContinuation;
    }

    public int getValue() {
        return mValue;
    }

    @Override
    public int compareTo(@NonNull NextfareTransactionRecord rhs) {
        // Group by journey, then by timestamp.
        // First trip in a journey goes first, and should (generally) be in pairs.

        if (rhs.mJourney == this.mJourney) {
            return Long.valueOf(this.mTimestamp.getTimeInMillis()).compareTo(rhs.mTimestamp.getTimeInMillis());
        } else {
            return (Integer.valueOf(this.mJourney)).compareTo(rhs.mJourney);
        }

    }
}
