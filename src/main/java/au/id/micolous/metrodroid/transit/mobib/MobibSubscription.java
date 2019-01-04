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
import android.support.annotation.Nullable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

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
            new En1545FixedHex(CONTRACT_UNKNOWN_A, 41),
            En1545FixedInteger.date(CONTRACT_SALE),
            new En1545FixedHex(CONTRACT_UNKNOWN_B, 177)
    );

    private final boolean mIsSubscription;

    MobibSubscription(ImmutableByteArray dataSub, Integer ctr) {
        super(dataSub, FIELDS, ctr);
        if(ctr != 0x2f02){
            // Ticket
            mIsSubscription = false;
        } else {
            // Subscription
            mIsSubscription = true;
        }
    }

    @Override
    public Integer getRemainingTripCount() {
        return mIsSubscription ? null : mCounter;
    }

    @Nullable
    @Override
    public String getSubscriptionName() {
        if (mIsSubscription)
            return Utils.localizeString(R.string.daily_subscription);
        return Utils.localizeString(R.string.single_trips);
    }

    @Override
    protected En1545Lookup getLookup() {
        return MobibLookup.getInstance();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(mIsSubscription ? 1 : 0);
    }

    private MobibSubscription(Parcel parcel) {
        super(parcel);
        mIsSubscription = parcel.readInt() == 1;
    }
}
