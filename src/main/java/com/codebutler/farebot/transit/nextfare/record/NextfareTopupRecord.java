/*
 * NextfareTopupRecord.java
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

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * Top-up record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC#top-up
 */
public class NextfareTopupRecord extends NextfareRecord implements Parcelable {
    private static final String TAG = "NextfareTopupRecord";
    private GregorianCalendar mTimestamp;
    private int mCredit;
    private int mStation;
    private int mChecksum;
    private boolean mAutomatic;

    public static final Creator<NextfareTopupRecord> CREATOR = new Creator<NextfareTopupRecord>() {
        @Override
        public NextfareTopupRecord createFromParcel(Parcel in) {
            return new NextfareTopupRecord(in);
        }

        @Override
        public NextfareTopupRecord[] newArray(int size) {
            return new NextfareTopupRecord[size];
        }
    };

    public static NextfareTopupRecord recordFromBytes(byte[] input) {
        //if ((input[0] != 0x01 && input[0] != 0x31) || input[1] != 0x01) throw new AssertionError("Not a topup record");

        NextfareTopupRecord record = new NextfareTopupRecord();

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mTimestamp = NextfareUtil.unpackDate(ts);

        byte[] credit = Utils.reverseBuffer(input, 6, 2);
        record.mCredit = Utils.byteArrayToInt(credit) & 0x7FFF;

        byte[] station = Utils.reverseBuffer(input, 12, 2);
        record.mStation = Utils.byteArrayToInt(station);

        byte[] checksum = Utils.reverseBuffer(input, 14, 2);
        record.mChecksum = Utils.byteArrayToInt(checksum);

        record.mAutomatic = input[0] == 0x31;

        Log.d(TAG, "@" + Utils.isoDateTimeFormat(record.mTimestamp) + ": " + record.mCredit + " cents, station " + record.mStation + ", " + (record.mAutomatic ? "auto" : "manual"));
        return record;
    }

    protected NextfareTopupRecord() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mTimestamp.getTimeInMillis());
        parcel.writeInt(mCredit);
        parcel.writeInt(mStation);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mAutomatic ? 1 : 0);
    }

    public NextfareTopupRecord(Parcel parcel) {
        mTimestamp = new GregorianCalendar();
        mTimestamp.setTimeInMillis(parcel.readLong());
        mCredit = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
        mAutomatic = parcel.readInt() == 1;
    }

    public GregorianCalendar getTimestamp() {
        return mTimestamp;
    }

    public int getCredit() {
        return mCredit;
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
}
