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
package au.id.micolous.metrodroid.card.cepas;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.util.Calendar;

import au.id.micolous.metrodroid.util.Utils;

@Root(name = "transaction")
public class CEPASTransaction implements Parcelable {
    public static final Parcelable.Creator<CEPASTransaction> CREATOR = new Parcelable.Creator<CEPASTransaction>() {
        public CEPASTransaction createFromParcel(Parcel source) {
            byte type = source.readByte();
            int amount = source.readInt();
            Calendar date = Utils.longToCalendar(source.readLong(), CEPASCard.TZ);
            String userData = source.readString();
            return new CEPASTransaction(type, amount, date, userData);
        }

        public CEPASTransaction[] newArray(int size) {
            return new CEPASTransaction[size];
        }
    };
    @Attribute(name = "type")
    private byte mType;
    @Attribute(name = "amount")
    private int mAmount;

    /**
     * This is the date expressed as seconds since the UNIX epoch. Metrodroid <= 2.9.34 used
     * this value, but this attribute is required to read old scans.
     *
     * Use {@link #mDate2} instead.
     */
    @Deprecated
    @Attribute(name = "date", required = false)
    private long mDate;

    /**
     * This is the date expressed as seconds since the CEPAS epoch (1995-01-01 00:00 SGT).
     * Metrodroid >= 2.9.35 uses this value, old versions use {@link #mDate} instead.
     */
    @Attribute(name = "date2", required = false)
    private Calendar mDate2;

    @Attribute(name = "user-data")
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
        mDate2 = CEPASCard.timestampToCalendar(timestamp);
        mDate = 0;

        byte[] userData = new byte[9];
        System.arraycopy(rawData, 8, userData, 0, 8);
        userData[8] = '\0';
        mUserData = new String(userData);
    }

    public CEPASTransaction(byte type, int amount, Calendar date, String userData) {
        mType = type;
        mAmount = amount;
        //noinspection deprecation
        mDate = 0;
        mDate2 = date;
        mUserData = userData;
    }

    private CEPASTransaction() { /* For XML Serializer */ }

    public TransactionType getType() {
        if (mType == 48)
            return TransactionType.MRT;
        if (mType == 117 || mType == 3)
            return TransactionType.TOP_UP;
        if (mType == 49)
            return TransactionType.BUS;
        if (mType == 118)
            return TransactionType.BUS_REFUND;
        if (mType == -16 || mType == 5)
            return TransactionType.CREATION;
        if (mType == 4)
            return TransactionType.SERVICE;
        if (mType == 1)
            return TransactionType.RETAIL;
        return TransactionType.UNKNOWN;
    }

    public int getAmount() {
        return mAmount;
    }

    @SuppressWarnings("deprecation")
    public Calendar getTimestamp() {
        if (mDate != 0) {
            // Compatibility for Metrodroid <= 2.9.34
            // Timestamps were stored as seconds since UNIX epoch.
            return CEPASCard.timestampToCalendar(mDate - 788947200 + (16 * 3600));
        }

        return mDate2;
    }

    public String getUserData() {
        return mUserData;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeByte(mType);
        parcel.writeInt(mAmount);
        parcel.writeLong(Utils.calendarToLong(mDate2));
        parcel.writeString(mUserData);
    }

    public int describeContents() {
        return 0;
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
