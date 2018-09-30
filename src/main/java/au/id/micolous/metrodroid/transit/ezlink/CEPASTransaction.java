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
            byte type = source.readByte();
            int amount = source.readInt();
            Calendar date = Utils.unparcelCalendar(source);
            String userData = source.readString();
            return new CEPASTransaction(type, amount, date, userData);
        }

        public CEPASTransaction[] newArray(int size) {
            return new CEPASTransaction[size];
        }
    };
    private byte mType;
    private int mAmount;
    private Calendar mDate;
    private String mUserData;

    public CEPASTransaction(byte[] rawData) {
        int tmp;

        mType = rawData[0];

        tmp = Utils.byteArrayToInt(rawData, 1, 3);
        /* Sign-extend the value */
        if (0 != (tmp & 0x800000))
            tmp |= 0xff000000;
        mAmount = tmp;

        /* Date is expressed "in seconds", but the epoch is January 1 1995, SGT */
        long timestamp = Utils.byteArrayToLong(rawData, 4, 4);
        mDate = EZLinkTransitData.timestampToCalendar(timestamp);

        byte[] userData = new byte[9];
        System.arraycopy(rawData, 8, userData, 0, 8);
        userData[8] = '\0';
        mUserData = new String(userData);
    }

    public CEPASTransaction(byte type, int amount, Calendar date, String userData) {
        mType = type;
        mAmount = amount;
        mDate = date;
        mUserData = userData;
    }

    private CEPASTransaction() { /* For XML Serializer */ }

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
