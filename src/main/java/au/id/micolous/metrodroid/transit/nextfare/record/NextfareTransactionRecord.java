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
    private final Calendar mTimestamp;
    private final int mMode;
    private final int mJourney;
    private final int mStation;
    private final int mValue;
    private final int mChecksum;
    private final boolean mContinuation;

    private NextfareTransactionRecord(Calendar timestamp, int mode, int journey,
                                      int station, int value, int checksum, boolean continuation) {
        mTimestamp = timestamp;
        mMode = mode;
        mJourney = journey;
        mStation = station;
        mValue = value;
        mChecksum = checksum;
        mContinuation = continuation;
    }

    private NextfareTransactionRecord(Parcel parcel) {
        mTimestamp = Utils.unparcelCalendar(parcel);
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
        // Minneapolis: input[0] == 0x89 unknown transaction type, no date, only a small number
        // around 100

        int transhead = (input[0] & 0xff);
        if (transhead == 0x89 || transhead == 0x71 || transhead == 0x79) {
            return null;
        }

        // Check if all the other data is null
        if (Utils.byteArrayToLong(input, 1, 8) == 0L) {
            Log.d(TAG, "Null transaction record, skipping");
            return null;
        }


        int mode = Utils.byteArrayToInt(input, 1, 1);

        Calendar timestamp = NextfareUtil.unpackDate(input, 2, timeZone);
        int journey = Utils.byteArrayToIntReversed(input, 5, 2) >> 5;

        boolean continuation = (Utils.byteArrayToIntReversed(input, 5, 2) & 0x10) > 1;

        int value = Utils.byteArrayToIntReversed(input, 7 ,2);
        if (value > 0x8000) {
            value = -(value & 0x7fff);
        }

        int station = Utils.byteArrayToIntReversed(input, 12, 2);
        int checksum = Utils.byteArrayToIntReversed(input, 14, 2);

        NextfareTransactionRecord record = new NextfareTransactionRecord(
                timestamp, mode, journey, station, value, checksum, continuation);

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
        Utils.parcelCalendar(parcel, mTimestamp);
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
            return Long.compare(this.mTimestamp.getTimeInMillis(), rhs.mTimestamp.getTimeInMillis());
        } else {
            return Integer.compare(this.mJourney, rhs.mJourney);
        }

    }
}
