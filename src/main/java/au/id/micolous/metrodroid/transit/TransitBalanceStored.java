/*
 * TransitBalanceStored.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;

import au.id.micolous.metrodroid.util.Utils;

public class TransitBalanceStored extends TransitBalance implements Parcelable {
    private final Calendar mValidityStart;
    private final TransitCurrency mBal;
    private final String mName;
    private final Calendar mExpiry;

    public TransitBalanceStored(TransitCurrency bal) {
        this(bal, null, null, null);
    }

    public TransitBalanceStored(TransitCurrency bal, String name, Calendar expiry) {
        this(bal, name, null, expiry);
    }

    public TransitBalanceStored(TransitCurrency bal, String name, Calendar validityStart, Calendar expiry) {
        mBal = bal;
        mName = name;
        mExpiry = expiry;
        mValidityStart = validityStart;
    }

    protected TransitBalanceStored(Parcel in) {
        mBal = in.readParcelable(TransitCurrency.class.getClassLoader());
        if (in.readInt() != 0) {
            mName = in.readString();
        } else
            mName = null;
        mExpiry = Utils.unparcelCalendar(in);
        mValidityStart = Utils.unparcelCalendar(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mBal, flags);
        if (mName != null) {
            dest.writeInt(1);
            dest.writeString(mName);
        } else
            dest.writeInt(0);
        Utils.parcelCalendar(dest, mExpiry);
        Utils.parcelCalendar(dest, mValidityStart);
    }

    public static final Creator<TransitBalanceStored> CREATOR = new Creator<TransitBalanceStored>() {
        @Override
        public TransitBalanceStored createFromParcel(Parcel in) {
            return new TransitBalanceStored(in);
        }

        @Override
        public TransitBalanceStored[] newArray(int size) {
            return new TransitBalanceStored[size];
        }
    };

    @Override
    public TransitCurrency getBalance() {
        return mBal;
    }

    @Override
    public Calendar getValidFrom() {
        return mValidityStart;
    }

    @Override
    public Calendar getValidTo() {
        return mExpiry;
    }

    @Override
    public String getName() {
        return mName;
    }
}
