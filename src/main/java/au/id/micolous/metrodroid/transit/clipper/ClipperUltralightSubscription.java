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

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

class ClipperUltralightSubscription extends Subscription {
    private final int mProduct;
    private final int mTripsRemaining;
    private final int mTransferExpiry;

    public ClipperUltralightSubscription(int product, int tripsRemaining, int transferExpiry) {
        mProduct = product;
        mTripsRemaining = tripsRemaining;
        mTransferExpiry = transferExpiry;
    }

    public ClipperUltralightSubscription(Parcel in) {
        mProduct = in.readInt();
        mTripsRemaining = in.readInt();
        mTransferExpiry = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mProduct);
        dest.writeInt(mTripsRemaining);
        dest.writeInt(mTransferExpiry);
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
        // TODO: i18n
        switch (mProduct & 0xf) {
            case 0x3:
                return "Single-ride";
            case 0x4:
                return "Round trip";
            case 0x5:
                return "Single-ride (senior)";
            case 0x6:
                return "Round trip (senior)";
            case 0x7:
                return "Single-ride (RTC)";
            case 0x8:
                return "Round trip (RTC)";
            case 0x9:
                return "Single-ride (youth)";
            case 0xa:
                return "Round trip (youth)";
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

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        List<ListItem> li = super.getInfo();
        if (mTransferExpiry != 0) {
            if (li == null)
                li = new ArrayList<>();
            li.add(new ListItem(R.string.clipper_free_transfers_until,
                    Utils.dateTimeFormat(TripObfuscator.maybeObfuscateTS(
                            ClipperTransitData.clipperTimestampToCalendar(mTransferExpiry * 60L)))));
        }
        return li;
    }
}
