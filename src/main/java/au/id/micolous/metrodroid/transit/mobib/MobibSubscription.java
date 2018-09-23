/*
 * MobibSubscription.java
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
package au.id.micolous.metrodroid.transit.mobib;

import android.os.Parcel;
import android.support.annotation.NonNull;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;
import au.id.micolous.metrodroid.util.Utils;

class MobibSubscription extends En1545Subscription {

    public static final Creator<MobibSubscription> CREATOR = new Creator<MobibSubscription>() {
        @NonNull
        public MobibSubscription createFromParcel(Parcel parcel) {
            return new MobibSubscription(parcel);
        }

        @NonNull
        public MobibSubscription[] newArray(int size) {
            return new MobibSubscription[size];
        }
    };
    private static final En1545Field FIELDS = new En1545Container(
            new En1545FixedHex("UnknownA", 41),
            En1545FixedInteger.date("ContractSale"),
            new En1545FixedHex("UnknownD", 177)
    );

    private final int mTicketsRemaining;
    private final boolean mIsSubscription;

    public MobibSubscription(byte[] dataSub, int dataCtr, int num) {
        super(dataSub, FIELDS, num);
        if(dataCtr != 0x2f02){
            // Ticket
            mIsSubscription = false;
            mTicketsRemaining = dataCtr;
        } else {
            // Subscription
            mIsSubscription = true;
            mTicketsRemaining = 0;
        }
    }

    @Override
    protected Integer getCounter() {
        return mIsSubscription ? null : mTicketsRemaining;
    }

    @Override
    public String getSubscriptionName() {
        if (mIsSubscription)
            return Utils.localizeString(R.string.en1545_daily_subscription);
        return Utils.localizeString(R.string.en1545_single_trips);
    }

    @Override
    protected En1545Lookup getLookup() {
        return MobibLookup.getInstance();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(mTicketsRemaining);
        parcel.writeInt(mIsSubscription ? 1 : 0);
    }

    private MobibSubscription(Parcel parcel) {
        super(parcel);
        mTicketsRemaining = parcel.readInt();
        mIsSubscription = parcel.readInt() == 1;
    }
}
