/*
 * CEPASTransaction.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2013-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.ezlink;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;

import au.id.micolous.metrodroid.util.Utils;

public class CEPASTransaction implements Parcelable {
    public static final Parcelable.Creator<CEPASTransaction> CREATOR = new Parcelable.Creator<CEPASTransaction>() {
        public CEPASTransaction createFromParcel(Parcel source) {
            return new CEPASTransaction(source);
        }

        public CEPASTransaction[] newArray(int size) {
            return new CEPASTransaction[size];
        }
    };
    private final byte mType;
    private final int mAmount;
    private final Calendar mDate;
    private final String mUserData;

    public CEPASTransaction(byte[] rawData) {
        mType = rawData[0];

        mAmount = Utils.getBitsFromBufferSigned(rawData, 8, 24);

        /* Date is expressed "in seconds", but the epoch is January 1 1995, SGT */
        long timestamp = Utils.byteArrayToLong(rawData, 4, 4);
        mDate = EZLinkTransitData.timestampToCalendar(timestamp);

        byte[] userData = new byte[9];
        System.arraycopy(rawData, 8, userData, 0, 8);
        userData[8] = '\0';
        mUserData = new String(userData, Utils.getASCII());
    }

    private CEPASTransaction(Parcel source) {
        mType = source.readByte();
        mAmount = source.readInt();
        mDate = Utils.unparcelCalendar(source);
        mUserData = source.readString();
    }

    public TransactionType getType() {
        return getType(mType);
    }

    public int getAmount() {
        return mAmount;
    }

    @SuppressWarnings("deprecation")
    public Calendar getTimestamp() {
        return mDate;
    }

    public String getUserData() {
        return mUserData;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeByte(mType);
        parcel.writeInt(mAmount);
        Utils.parcelCalendar(parcel, mDate);
        parcel.writeString(mUserData);
    }

    public int describeContents() {
        return 0;
    }

    public static TransactionType getType(byte type) {
        if (type == 48)
            return TransactionType.MRT;
        if (type == 117 || type == 3)
            return TransactionType.TOP_UP;
        if (type == 49)
            return TransactionType.BUS;
        if (type == 118)
            return TransactionType.BUS_REFUND;
        if (type == -16 || type == 5)
            return TransactionType.CREATION;
        if (type == 4)
            return TransactionType.SERVICE;
        if (type == 1)
            return TransactionType.RETAIL;
        return TransactionType.UNKNOWN;
    }

    public enum TransactionType {
        MRT,
        TOP_UP, /* Old MRT transit info is unhyphenated - renamed from OLD_MRT to TOP_UP, as it seems like the code has been repurposed. */
        BUS,
        BUS_REFUND,
        CREATION,
        RETAIL,
        SERVICE,
        UNKNOWN,
    }
}
