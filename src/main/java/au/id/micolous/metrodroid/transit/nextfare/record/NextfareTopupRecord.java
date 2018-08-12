/*
 * NextfareTopupRecord.java
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
import android.util.Log;

import au.id.micolous.metrodroid.transit.nextfare.NextfareUtil;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Top-up record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
public class NextfareTopupRecord extends NextfareRecord implements Parcelable {
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
    private static final String TAG = "NextfareTopupRecord";
    private Calendar mTimestamp;
    private int mCredit;
    private int mStation;
    private int mChecksum;
    private boolean mAutomatic;

    protected NextfareTopupRecord() {
    }

    public NextfareTopupRecord(Parcel parcel) {
        mTimestamp = Utils.unparcelCalendar(parcel);
        mCredit = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
        mAutomatic = parcel.readInt() == 1;
    }

    public static NextfareTopupRecord recordFromBytes(byte[] input, TimeZone timeZone) {
        //if ((input[0] != 0x01 && input[0] != 0x31) || input[1] != 0x01) throw new AssertionError("Not a topup record");

        // Check if all the other data is null
        if (Utils.byteArrayToLong(input, 2, 6) == 0L) {
            Log.d(TAG, "Null top-up record, skipping");
            return null;
        }

        NextfareTopupRecord record = new NextfareTopupRecord();

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mTimestamp = NextfareUtil.unpackDate(ts, timeZone);

        byte[] credit = Utils.reverseBuffer(input, 6, 2);
        record.mCredit = Utils.byteArrayToInt(credit) & 0x7FFF;

        byte[] station = Utils.reverseBuffer(input, 12, 2);
        record.mStation = Utils.byteArrayToInt(station);

        byte[] checksum = Utils.reverseBuffer(input, 14, 2);
        record.mChecksum = Utils.byteArrayToInt(checksum);

        record.mAutomatic = input[0] == 0x31;

        Log.d(TAG, String.format("@%s: %d cents, station %d, %s",
                Utils.isoDateTimeFormat(record.mTimestamp), record.mCredit, record.mStation, (record.mAutomatic ? "auto" : "manual")));
        return record;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Utils.parcelCalendar(parcel, mTimestamp);
        parcel.writeInt(mCredit);
        parcel.writeInt(mStation);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mAutomatic ? 1 : 0);
    }

    public Calendar getTimestamp() {
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
