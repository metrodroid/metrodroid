/*
 * ErgTransaction.java
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.erg;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Represents a transaction on an ERG MIFARE Classic card.
 */
public class ErgTransaction extends Transaction {
    public static final Parcelable.Creator<ErgTransaction> CREATOR = new Parcelable.Creator<ErgTransaction>() {

        public ErgTransaction createFromParcel(Parcel in) {
            return new ErgTransaction(in);
        }

        public ErgTransaction[] newArray(int size) {
            return new ErgTransaction[size];
        }
    };

    private final Calendar mEpoch;
    protected final ErgPurseRecord mPurse;

    @NonNull
    private final String mCurrency;

    public ErgTransaction(ErgPurseRecord purse, GregorianCalendar epoch, @NonNull String currency) {
        mPurse = purse;
        mEpoch = epoch;
        mCurrency = currency;
    }

    protected ErgTransaction(Parcel parcel) {
        mPurse = new ErgPurseRecord(parcel);
        mEpoch = Utils.unparcelCalendar(parcel);
        mCurrency = parcel.readString();
    }

    // Implemented functionality.
    @Override
    public Calendar getTimestamp() {
        GregorianCalendar ts = new GregorianCalendar();
        ts.setTimeInMillis(mEpoch.getTimeInMillis());
        ts.add(Calendar.DATE, mPurse.getDay());
        ts.add(Calendar.MINUTE, mPurse.getMinute());

        return ts;
    }

    @Override
    protected boolean isTapOff() {
        return false;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        int o = mPurse.getTransactionValue();
        if (mPurse.isCredit()) {
            o *= -1;
        }

        return new TransitCurrency(o, mCurrency);
    }

    @Override
    protected boolean isSameTrip(@NonNull Transaction other) {
        return false;
    }

    @Override
    protected boolean isTapOn() {
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean isTransfer() {
        // TODO
        return false;
    }

    @NonNull
    @Override
    public List<String> getRouteNames() {
        return Collections.singletonList(Utils.intToHex(mPurse.getRoute()));
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        mPurse.writeToParcel(parcel, i);
        Utils.parcelCalendar(parcel, mEpoch);
        parcel.writeString(mCurrency);
    }
}
