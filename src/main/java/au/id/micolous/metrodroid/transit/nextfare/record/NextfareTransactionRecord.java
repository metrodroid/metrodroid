/*
 * NextfareTapRecord.java
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

package au.id.micolous.metrodroid.transit.nextfare.record;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import au.id.micolous.metrodroid.transit.nextfare.NextfareUtil;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Tap record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
public class NextfareTransactionRecord extends NextfareRecord implements Parcelable, Comparable<NextfareTransactionRecord> {
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
    private static final String TAG = "NextfareTxnRecord";
    private Calendar mTimestamp;
    private int mMode;
    private int mJourney;
    private int mStation;
    private int mValue;
    private int mChecksum;
    private boolean mContinuation;

    protected NextfareTransactionRecord() {
    }

    public NextfareTransactionRecord(Parcel parcel) {
        TimeZone tz = TimeZone.getTimeZone(parcel.readString());
        mTimestamp = Utils.longToCalendar(parcel.readLong(), tz);
        mMode = parcel.readInt();
        mJourney = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
        mContinuation = parcel.readInt() == 1;
        mValue = parcel.readInt();
    }

    public static NextfareTransactionRecord recordFromBytes(byte[] input, TimeZone timeZone) {
        //if (input[0] != 0x31) throw new AssertionError("not a tap record");

        // LAX:      input[0] == 0x05 for "Travel Pass" trips.
        // SEQ, LAX: input[0] == 0x31 for "Stored Value" trips / transfers
        // LAX:      input[0] == 0x41 for "Travel Pass" sale.
        // LAX:      input[0] == 0x71 for "Stored Value" sale -- effectively recorded twice
        // SEQ, LAX: input[0] == 0x79 for "Stored Value" sale

        if (input[0] > 0x70) {
            return null;
        }

        // Check if all the other data is null
        if (Utils.byteArrayToLong(input, 1, 8) == 0L) {
            Log.d(TAG, "Null transaction record, skipping");
            return null;
        }

        NextfareTransactionRecord record = new NextfareTransactionRecord();

        record.mMode = Utils.byteArrayToInt(input, 1, 1);

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mTimestamp = NextfareUtil.unpackDate(ts, timeZone);

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

        Log.d(TAG, String.format("@%s: mode %d, station %d, value %d, journey %d, %s",
                Utils.isoDateTimeFormat(record.mTimestamp), record.mMode, record.mStation, record.mValue,
                record.mJourney, (record.mContinuation ? "continuation" : "new trip")));

        return record;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mTimestamp.getTimeZone().getID());
        parcel.writeLong(Utils.calendarToLong(mTimestamp));
        parcel.writeInt(mMode);
        parcel.writeInt(mJourney);
        parcel.writeInt(mStation);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mContinuation ? 1 : 0);
        parcel.writeInt(mValue);
    }

    public int getMode() {
        return mMode;
    }

    public Calendar getTimestamp() {
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
