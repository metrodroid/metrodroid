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

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.util.Utils;

class MobibSubscription extends Subscription {

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

    private final int mExpiry;
    private final int mPurchase;
    private final int mTicketsRemaining;
    private final boolean mIsSubscription;
    private final int mId;

    public MobibSubscription(byte[] dataSub, int dataCtr, int num) {
        mPurchase = Utils.getBitsFromBuffer(dataSub, 40, 15);
        mExpiry = Utils.getBitsFromBuffer(dataSub, 60, 15);
        if(dataCtr != 0x2f02){
            // Ticket
            mIsSubscription = false;
            mTicketsRemaining = dataCtr;
        } else {
            // Subscription
            mIsSubscription = true;
            mTicketsRemaining = 0;
        }
        mId = num;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public Calendar getValidFrom() {
        return MobibTransitData.parseTime(mPurchase, 0);
    }

    @Override
    public Calendar getValidTo() {
        if (mIsSubscription) {
            return MobibTransitData.parseTime(mExpiry, 0);
        }
        return MobibTransitData.parseTime(mPurchase, 0);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return null;
    }

    @Override
    public int getMachineId() {
        return 0;
    }

    @Override
    public String getSubscriptionName() {
        if (mIsSubscription)
            return Utils.localizeString(R.string.mobib_daily_subscription);
        return Utils.localizeString(R.string.mobib_single_trips);
    }

    @Override
    public String getActivation() {
        if (mIsSubscription)
            return null;
        return Utils.localizePlural(R.plurals.trips_remaining, mTicketsRemaining, mTicketsRemaining);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mExpiry);
        parcel.writeInt(mPurchase);
        parcel.writeInt(mTicketsRemaining);
        parcel.writeInt(mIsSubscription ? 1 : 0);
        parcel.writeInt(mId);
    }

    public MobibSubscription(Parcel parcel) {
        mExpiry = parcel.readInt();
        mPurchase = parcel.readInt();
        mTicketsRemaining = parcel.readInt();
        mIsSubscription = parcel.readInt() == 1;
        mId = parcel.readInt();
    }
}
