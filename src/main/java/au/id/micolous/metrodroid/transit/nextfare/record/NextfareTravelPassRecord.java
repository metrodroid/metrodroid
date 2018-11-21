/*
 * NextfareTravelPassRecord.java
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
 * Travel pass record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */

public class NextfareTravelPassRecord extends NextfareRecord implements Parcelable, Comparable<NextfareTravelPassRecord> {
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
    private static final String TAG = "NextfareTravelPassRec";
    private Calendar mExpiry;
    private int mChecksum;
    private boolean mAutomatic;
    private int mVersion;

    protected NextfareTravelPassRecord() {
    }

    public NextfareTravelPassRecord(Parcel parcel) {
        mExpiry = Utils.unparcelCalendar(parcel);
        mChecksum = parcel.readInt();
        mAutomatic = parcel.readInt() == 1;
    }

    public static NextfareTravelPassRecord recordFromBytes(byte[] input, TimeZone timeZone) {
        //if ((input[0] != 0x01 && input[0] != 0x31) || input[1] != 0x01) throw new AssertionError("Not a topup record");
        if (Utils.byteArrayToInt(input, 2, 4) == 0) {
            // Timestamp is null, ignore.
            return null;
        }

        NextfareTravelPassRecord record = new NextfareTravelPassRecord();
        record.mVersion = Utils.byteArrayToInt(input, 13, 1);

        record.mExpiry = NextfareUtil.unpackDate(input, 2, timeZone);
        record.mChecksum = Utils.byteArrayToIntReversed(input, 14, 2);

        //noinspection StringConcatenation
        Log.d(TAG, "@" + Utils.isoDateTimeFormat(record.mExpiry) + ": version " + record.mVersion);

        if (record.mVersion == 0) {
            // There is no travel pass loaded on to this card.
            return null;
        }
        return record;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Utils.parcelCalendar(parcel, mExpiry);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mAutomatic ? 1 : 0);
    }

    public Calendar getTimestamp() {
        return mExpiry;
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
        return Integer.valueOf(rhs.mVersion).compareTo(this.mVersion);
    }
}
