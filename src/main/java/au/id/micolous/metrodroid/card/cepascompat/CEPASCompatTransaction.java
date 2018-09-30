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
package au.id.micolous.metrodroid.card.cepascompat;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData;
import au.id.micolous.metrodroid.util.Utils;

// This file is only for reading old dumps
@Root(name = "transaction")
public class CEPASCompatTransaction implements Parcelable {
    public static final Parcelable.Creator<CEPASCompatTransaction> CREATOR = new Parcelable.Creator<CEPASCompatTransaction>() {
        public CEPASCompatTransaction createFromParcel(Parcel source) {
            byte type = source.readByte();
            int amount = source.readInt();
            Calendar date = Utils.unparcelCalendar(source);
            String userData = source.readString();
            return new CEPASCompatTransaction(type, amount, date, userData);
        }

        public CEPASCompatTransaction[] newArray(int size) {
            return new CEPASCompatTransaction[size];
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

    public CEPASCompatTransaction(byte type, int amount, Calendar date, String userData) {
        mType = type;
        mAmount = amount;
        //noinspection deprecation
        mDate = 0;
        mDate2 = date;
        mUserData = userData;
    }

    private CEPASCompatTransaction() { /* For XML Serializer */ }

    public byte getType() {
        return mType;
    }

    public int getAmount() {
        return mAmount;
    }

    @SuppressWarnings("deprecation")
    public Calendar getTimestamp() {
        if (mDate != 0) {
            // Compatibility for Metrodroid <= 2.9.34
            // Timestamps were stored as seconds since UNIX epoch.
            return EZLinkTransitData.timestampToCalendar(mDate - 788947200 + (16 * 3600));
        }

        // XML serializer resets time zone to device timezone
        GregorianCalendar g = new GregorianCalendar(EZLinkTransitData.TZ);
        g.setTimeInMillis(mDate2.getTimeInMillis());
        return g;
    }

    public String getUserData() {
        return mUserData;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeByte(mType);
        parcel.writeInt(mAmount);
        Utils.parcelCalendar(parcel, mDate2);
        parcel.writeString(mUserData);
    }

    public int describeContents() {
        return 0;
    }
}
