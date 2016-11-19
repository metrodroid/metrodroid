/*
 * NextfareConfigRecord.java
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
import android.util.Log;

import com.codebutler.farebot.util.Utils;

/**
 * Represents a configuration record on Nextfare MFC.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
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

