package com.codebutler.farebot.transit.nextfare.record;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.codebutler.farebot.util.Utils;

/**
 * Represents a configuration record on Nextfare MFC.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC#configuration
 */

public class NextfareConfigRecord extends NextfareRecord implements Parcelable {
    private static final String TAG = "NextfareConfigRecord";

    private int mTicketType;


    public static final Creator<NextfareConfigRecord> CREATOR = new Creator<NextfareConfigRecord>() {
        @Override
        public NextfareConfigRecord createFromParcel(Parcel in) {
            return new NextfareConfigRecord(in);
        }

        @Override
        public NextfareConfigRecord[] newArray(int size) {
            return new NextfareConfigRecord[size];
        }
    };

    public static NextfareConfigRecord recordFromBytes(byte[] input) {
        //if (input[0] != 0x01) throw new AssertionError();

        NextfareConfigRecord record = new NextfareConfigRecord();

        // Treat ticket type as little-endian
        byte[] ticketType = Utils.reverseBuffer(input, 8, 2);
        record.mTicketType = Utils.byteArrayToInt(ticketType, 0, 2);

        Log.d(TAG, "Config ticket type = " + record.mTicketType);
        return record;
    }

    protected NextfareConfigRecord() {}

    public int getTicketType() {
        return mTicketType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mTicketType);
    }

    public NextfareConfigRecord(Parcel p) {
        mTicketType = p.readInt();
    }
}

