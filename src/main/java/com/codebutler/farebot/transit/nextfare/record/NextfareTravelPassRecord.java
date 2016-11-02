/*
 * NextfareTravelPassRecord.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
 * Travel pass record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC#travel-pass
 */

public class NextfareTravelPassRecord extends NextfareRecord implements Parcelable, Comparable<NextfareTravelPassRecord> {
    private static final String TAG = "NextfareTravelPassRec";
    private GregorianCalendar mExpiry;
    private int mStation;
    private int mChecksum;
    private boolean mAutomatic;
    private int mVersion;

    public static final Creator<NextfareTravelPassRecord> CREATOR = new Creator<NextfareTravelPassRecord>() {
        @Override
        public NextfareTravelPassRecord createFromParcel(Parcel in) {
            return new NextfareTravelPassRecord(in);
        }

        @Override
        public NextfareTravelPassRecord[] newArray(int size) {
            return new NextfareTravelPassRecord[size];
        }
    };

    public static NextfareTravelPassRecord recordFromBytes(byte[] input) {
        //if ((input[0] != 0x01 && input[0] != 0x31) || input[1] != 0x01) throw new AssertionError("Not a topup record");

        NextfareTravelPassRecord record = new NextfareTravelPassRecord();
        record.mVersion = Utils.byteArrayToInt(input, 13, 1);

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mExpiry = NextfareUtil.unpackDate(ts);

        byte[] station = Utils.reverseBuffer(input, 12, 2);
        record.mStation = Utils.byteArrayToInt(station);

        byte[] checksum = Utils.reverseBuffer(input, 14, 2);
        record.mChecksum = Utils.byteArrayToInt(checksum);

        Log.d(TAG, "@" + Utils.isoDateTimeFormat(record.mExpiry) + ": version " + record.mVersion + ", station " + record.mStation + ", ");

        if (record.mVersion == 0 && record.mStation == 0) {
            // There is no travel pass loaded on to this card.
            return null;
        }
        return record;
    }

    protected NextfareTravelPassRecord() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mExpiry.getTimeInMillis());
        parcel.writeInt(mStation);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mAutomatic ? 1 : 0);
    }

    public NextfareTravelPassRecord(Parcel parcel) {
        mExpiry = new GregorianCalendar();
        mExpiry.setTimeInMillis(parcel.readLong());
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
        mAutomatic = parcel.readInt() == 1;
    }

    public GregorianCalendar getTimestamp() {
        return mExpiry;
    }


    public int getStation() {
        return mStation;
    }

    public int getChecksum() {
        return mChecksum;
    }

    public boolean getAutomatic() {
        return mAutomatic;
    }

    @Override
    public int compareTo(@NonNull NextfareTravelPassRecord rhs) {
        // So sorting works, we reverse the order so highest number is first.
        return Integer.valueOf(rhs.mVersion).compareTo(this.mVersion);    }
}
