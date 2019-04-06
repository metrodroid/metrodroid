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
import android.support.annotation.Nullable;
import android.util.Log;

import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Tap record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
public class NextfareTransactionRecord
        extends Transaction
        implements Parcelable {
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
    private final TransitCurrency mFare;
    private final int mChecksum;
    private final boolean mContinuation;
    private final boolean mAgencyTapsOff;

    protected NextfareTransactionRecord(
            Calendar timestamp, int mode, int journey, int station, TransitCurrency fare,
            int checksum, boolean continuation, boolean agencyTapsOff) {
        mTimestamp = timestamp;
        mMode = mode;
        mJourney = journey;
        mStation = station;
        mFare = fare;
        mChecksum = checksum;
        mContinuation = continuation;
        mAgencyTapsOff = agencyTapsOff;
    }

    protected NextfareTransactionRecord(Parcel parcel) {
        mTimestamp = Utils.unparcelCalendar(parcel);
        mMode = parcel.readInt();
        mJourney = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
        mContinuation = parcel.readInt() == 1;
        mFare = parcel.readParcelable(getClass().getClassLoader());
        mAgencyTapsOff = parcel.readInt() == 1;
    }

    @Nullable
    public static NextfareTransactionRecord recordFromBytes(
            ImmutableByteArray input, TimeZone timeZone, boolean agencyTapsOff,
            @NonNull TransitCurrency.Builder currency) {
        //if (input[0] != 0x31) throw new AssertionError("not a tap record");

        // LAX:      input[0] == 0x05 for "Travel Pass" trips.
        // SEQ, LAX: input[0] == 0x31 for "Stored Value" trips / transfers
        // LAX:      input[0] == 0x41 for "Travel Pass" sale.
        // LAX:      input[0] == 0x71 for "Stored Value" sale -- effectively recorded twice
        // SEQ, LAX: input[0] == 0x79 for "Stored Value" sale
        // Minneapolis: input[0] == 0x89 unknown transaction type, no date, only a small number
        // around 100

        int transhead = (input.get(0) & 0xff);
        if (transhead == 0x89 || transhead == 0x71 || transhead == 0x79) {
            return null;
        }

        // Check if all the other data is null
        if (input.byteArrayToLong(1, 8) == 0L) {
            Log.d(TAG, "Null transaction record, skipping");
            return null;
        }


        int mode = input.byteArrayToInt(1, 1);

        Calendar timestamp = NextfareRecord.unpackDate(input, 2, timeZone);
        int journey = input.byteArrayToIntReversed(5, 2) >> 5;

        boolean continuation = (input.byteArrayToIntReversed(5, 2) & 0x10) > 1;

        int value = input.byteArrayToIntReversed(7 ,2);
        if (value > 0x8000) {
            value = -(value & 0x7fff);
        }

        TransitCurrency fare = currency.build(-value);
        int station = input.byteArrayToIntReversed(12, 2);
        int checksum = input.byteArrayToIntReversed(14, 2);

        NextfareTransactionRecord record = new NextfareTransactionRecord(
                timestamp, mode, journey, station, fare, checksum, continuation,
                agencyTapsOff);

        Log.d(TAG, String.format("@%s: mode %d, station %d, value %s, journey %d, %s",
                Utils.isoDateTimeFormat(record.mTimestamp), record.mMode, record.mStation,
                record.mFare, record.mJourney,
                (record.mContinuation ? "continuation" : "new trip")));

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
        parcel.writeParcelable(mFare, i);
        parcel.writeInt(mAgencyTapsOff ? 1 : 0);
    }

    public int getModeID() {
        return mMode;
    }

    @Override
    protected boolean shouldBeMerged(@NonNull Transaction other) {
        if (!(other instanceof NextfareTransactionRecord)) {
            return false;
        }

        return shouldBeMerged((NextfareTransactionRecord)other);
    }

    protected boolean shouldBeMerged(@NonNull NextfareTransactionRecord other) {
        // FIXME: We can't differentiate tap on/off properly
        return isSameTrip(other);
    }

    @Override
    protected boolean isSameTrip(@NonNull Transaction other) {
        if (!(other instanceof NextfareTransactionRecord)) {
            return false;
        }

        return isSameTrip((NextfareTransactionRecord)other);
    }

    protected boolean isSameTrip(@NonNull NextfareTransactionRecord other) {
        return (agencyTapsOff() &&
                (getModeID() == other.getModeID()) &&
                (getJourney() == other.getJourney()));
    }

    @Override
    protected boolean isTapOn() {
        // This version of the method is false -- we don't actually check for a tap-off bit yet on
        // nextfare.  But the only user is #isSameTrip(), which we've overridden.
        return true;
    }

    public Calendar getTimestamp() {
        return mTimestamp;
    }

    public int getJourney() {
        return mJourney;
    }

    @Override
    protected boolean isTapOff() {
        // This version of the method is false -- we don't actually check for a tap-off bit yet on
        // nextfare.  But the only user is #isSameTrip(), which we've overridden.
        return false;
    }

    public int getStationID() {
        return mStation;
    }

    public int getChecksum() {
        return mChecksum;
    }

    public boolean isContinuation() {
        return mContinuation;
    }

    @Override
    public TransitCurrency getFare() {
        return mFare;
    }

    public boolean agencyTapsOff() {
        return mAgencyTapsOff;
    }

    @Override
    public int compareTo(@NonNull Transaction rhs) {
        if (rhs instanceof NextfareTransactionRecord) {
            return compareTo((NextfareTransactionRecord)rhs);
        }

        return super.compareTo(rhs);
    }

    public int compareTo(@NonNull NextfareTransactionRecord rhs) {
        // Group by journey, then by timestamp.
        // First trip in a journey goes first, and should (generally) be in pairs.

        if (rhs.mJourney == this.mJourney) {
            return super.compareTo(rhs);
        } else {
            return Integer.compare(this.mJourney, rhs.mJourney);
        }

    }
}
