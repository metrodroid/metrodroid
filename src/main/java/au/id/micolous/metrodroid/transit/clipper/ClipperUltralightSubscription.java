/*
 * ClipperUltralightSubscription.java
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

package au.id.micolous.metrodroid.transit.clipper;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.util.Utils;

class ClipperUltralightSubscription extends Subscription {
    private final int mProduct;
    private final int mTripsRemaining;
    private final int mTransferExpiry;
    private final int mBaseDate;

    ClipperUltralightSubscription(int product, int tripsRemaining, int transferExpiry, int baseDate) {
        mProduct = product;
        mTripsRemaining = tripsRemaining;
        mTransferExpiry = transferExpiry;
        mBaseDate = baseDate;
    }

    ClipperUltralightSubscription(Parcel in) {
        mProduct = in.readInt();
        mTripsRemaining = in.readInt();
        mTransferExpiry = in.readInt();
        mBaseDate = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mProduct);
        dest.writeInt(mTripsRemaining);
        dest.writeInt(mTransferExpiry);
        dest.writeInt(mBaseDate);
    }

    public static final Creator<ClipperUltralightSubscription> CREATOR = new Creator<ClipperUltralightSubscription>() {
        @Override
        public ClipperUltralightSubscription createFromParcel(Parcel in) {
            return new ClipperUltralightSubscription(in);
        }

        @Override
        public ClipperUltralightSubscription[] newArray(int size) {
            return new ClipperUltralightSubscription[size];
        }
    };

    @Nullable
    @Override
    public String getSubscriptionName() {
        switch (mProduct & 0xf) {
            case 0x3:
                return Utils.localizeString(R.string.clipper_single,
                        Utils.localizeString(R.string.clipper_ticket_type_adult));
            case 0x4:
                return Utils.localizeString(R.string.clipper_return,
                        Utils.localizeString(R.string.clipper_ticket_type_adult));
            case 0x5:
                return Utils.localizeString(R.string.clipper_single,
                        Utils.localizeString(R.string.clipper_ticket_type_senior));
            case 0x6:
                return Utils.localizeString(R.string.clipper_return,
                        Utils.localizeString(R.string.clipper_ticket_type_senior));
            case 0x7:
                return Utils.localizeString(R.string.clipper_single,
                        Utils.localizeString(R.string.clipper_ticket_type_rtc));
            case 0x8:
                return Utils.localizeString(R.string.clipper_return,
                        Utils.localizeString(R.string.clipper_ticket_type_rtc));
            case 0x9:
                return Utils.localizeString(R.string.clipper_single,
                        Utils.localizeString(R.string.clipper_ticket_type_youth));
            case 0xa:
                return Utils.localizeString(R.string.clipper_return,
                        Utils.localizeString(R.string.clipper_ticket_type_youth));
            default:
                return Integer.toHexString(mProduct);
        }
    }

    @Nullable
    @Override
    public String getAgencyName(boolean isShort) {
        if ((mProduct >> 4) == 0x21)
            return ClipperData.getAgencyName(ClipperData.AGENCY_MUNI, isShort);
        return ClipperData.getAgencyName(mProduct >> 4, isShort);
    }

    @Nullable
    @Override
    public Integer getRemainingTripCount() {
        return mTripsRemaining == -1 ? null : mTripsRemaining;
    }

    @Override
    public SubscriptionState getSubscriptionState() {
        if (mTripsRemaining == -1) {
            return SubscriptionState.UNUSED;
        } else if (mTripsRemaining == 0) {
            return SubscriptionState.USED;
        } else if (mTripsRemaining > 0) {
            return SubscriptionState.STARTED;
        } else {
            return SubscriptionState.UNKNOWN;
        }
    }

    @Nullable
    @Override
    public Calendar getTransferEndTimestamp() {
        return ClipperTransitData.clipperTimestampToCalendar(mTransferExpiry * 60L);
    }

    @Nullable
    @Override
    public Calendar getValidTo() {
        return ClipperTransitData.clipperTimestampToCalendar(mBaseDate * 86400L);
    }

    @Override
    public boolean validToHasTime() {
        return false;
    }

    @Nullable
    @Override
    public Calendar getPurchaseTimestamp() {
        return ClipperTransitData.clipperTimestampToCalendar((mBaseDate - 89) * 86400L);
    }

    @Override
    public boolean purchaseTimestampHasTime() {
        return false;
    }
}
