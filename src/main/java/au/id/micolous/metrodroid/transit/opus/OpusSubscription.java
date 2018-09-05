/*
 * OpalSubscription.java
 *
 * Copyright 2018 Etienne Dubeau
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
package au.id.micolous.metrodroid.transit.opus;

import android.os.Parcel;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

class OpusSubscription extends Subscription {

    public static final Creator<OpusSubscription> CREATOR = new Creator<OpusSubscription>() {
        public OpusSubscription createFromParcel(Parcel parcel) {
            return new OpusSubscription(parcel);
        }

        public OpusSubscription[] newArray(int size) {
            return new OpusSubscription[size];
        }
    };

    private final int mAgency;
    private final int mExpiry;
    private final int mTicketsRemaining;
    private final boolean mIsSubscription;
    private final int mId;

    public OpusSubscription(byte[] dataSub, byte[] dataCtr, int num) {
        // Copied from LecteurOpus
        mAgency = Utils.getBitsFromBuffer(dataSub, 9, 8);
        if(Utils.getBitsFromBuffer(dataSub, 40, 16) == 0){
            // Ticket
            mIsSubscription = false;
            mExpiry = 0;
            mTicketsRemaining = Utils.getBitsFromBuffer(dataCtr, 16, 8);
        } else {
            // Subscription
            mIsSubscription = true;
            mExpiry = Utils.getBitsFromBuffer(dataSub, 47, 14);
            mTicketsRemaining = 0;
        }
        // end of copy
        mId = num;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public Calendar getValidFrom() {
        return null;
    }

    @Override
    public Calendar getValidTo() {
        if (mIsSubscription) {
            return OpusTransitData.parseTime(mExpiry, 0);
        }
        return null;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return StationTableReader.getOperatorName(OpusTrip.OPUS_STR, mAgency, isShort);
    }

    @Override
    public int getMachineId() {
        return 0;
    }

    @Override
    public String getSubscriptionName() {
        if (mIsSubscription)
            return Utils.localizeString(R.string.opus_subscription);
        return Utils.localizeString(R.string.opus_single_trips);
    }

    @Override
    public String getActivation() {
        if (mIsSubscription)
            return null;
        return Utils.localizePlural(R.plurals.trips_remaining, mTicketsRemaining, mTicketsRemaining);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mAgency);
        parcel.writeInt(mExpiry);
        parcel.writeInt(mTicketsRemaining);
        parcel.writeInt(mIsSubscription ? 1 : 0);
        parcel.writeInt(mId);
    }

    public OpusSubscription(Parcel parcel) {
        mAgency = parcel.readInt();
        mExpiry = parcel.readInt();
        mTicketsRemaining = parcel.readInt();
        mIsSubscription = parcel.readInt() == 1;
        mId = parcel.readInt();
    }
}
