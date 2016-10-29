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
import android.util.Log;

import com.codebutler.farebot.transit.nextfare.NextfareUtil;
import com.codebutler.farebot.util.Utils;

import java.util.GregorianCalendar;

/**
 * Tap record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC#tap-record
 */
public class NextfareTapRecord extends NextfareRecord implements Parcelable, Comparable<NextfareTapRecord> {
    private static final String TAG = "NextfareTapRecord";
    private GregorianCalendar mTimestamp;
    private int mMode;
    private int mJourney;
    private int mStation;
    private int mChecksum;
    private boolean mContinuation;


    public static final Creator<NextfareTapRecord> CREATOR = new Creator<NextfareTapRecord>() {
        @Override
        public NextfareTapRecord createFromParcel(Parcel in) {
            return new NextfareTapRecord(in);
        }

        @Override
        public NextfareTapRecord[] newArray(int size) {
            return new NextfareTapRecord[size];
        }
    };

    public static NextfareTapRecord recordFromBytes(byte[] input) {
        //if (input[0] != 0x31) throw new AssertionError("not a tap record");

        NextfareTapRecord record = new NextfareTapRecord();

        record.mMode = Utils.byteArrayToInt(input, 1, 1);

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mTimestamp = NextfareUtil.unpackDate(ts);

        byte[] journey = Utils.reverseBuffer(input, 5, 2);
        record.mJourney = Utils.byteArrayToInt(journey) >> 5;
        record.mContinuation = (Utils.byteArrayToInt(journey) & 0x10) > 1;

        byte[] station = Utils.reverseBuffer(input, 12, 2);
        record.mStation = Utils.byteArrayToInt(station);

        byte[] checksum = Utils.reverseBuffer(input, 14, 2);
        record.mChecksum = Utils.byteArrayToInt(checksum);

        Log.d(TAG, "@" + Utils.isoDateTimeFormat(record.mTimestamp) + ": mode " + record.mMode + ", station " + record.mStation + ", journey " + record.mJourney + ", " + (record.mContinuation ? "continuation" : "new trip"));
        return record;
    }

    protected NextfareTapRecord() {}

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
    }

    public NextfareTapRecord(Parcel parcel) {
        mTimestamp = new GregorianCalendar();
        mTimestamp.setTimeInMillis(parcel.readLong());
        mMode = parcel.readInt();
        mJourney = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
        mContinuation = parcel.readInt() == 1;
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

    @Override
    public int compareTo(NextfareTapRecord rhs) {
        // Group by journey, then by timestamp.
        // First trip in a journey goes first, and should (generally) be in pairs.
        if (rhs.mJourney == this.mJourney) {
            return this.mTimestamp.compareTo(rhs.mTimestamp);
        } else {
            return (new Integer(this.mJourney)).compareTo(rhs.mJourney);
        }
    }
}
