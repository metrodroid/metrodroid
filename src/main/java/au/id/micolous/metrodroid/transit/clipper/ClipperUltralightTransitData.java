/*
 * ClipperUltralightTransitData.java
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
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;
import org.jetbrains.annotations.NotNull;

public class ClipperUltralightTransitData extends TransitData {

    public static final Parcelable.Creator<ClipperUltralightTransitData> CREATOR = new Parcelable.Creator<ClipperUltralightTransitData>() {
        public ClipperUltralightTransitData createFromParcel(Parcel parcel) {
            return new ClipperUltralightTransitData(parcel);
        }

        public ClipperUltralightTransitData[] newArray(int size) {
            return new ClipperUltralightTransitData[size];
        }
    };

    private static final String NAME = "Clipper Ultralight";

    private final long mSerial;
    private final int mBaseDate;
    private final List<ClipperUltralightTrip> mTrips;
    private final int mType;
    private final ClipperUltralightSubscription mSub;

    public final static UltralightCardTransitFactory FACTORY = new UltralightCardTransitFactory() {
        @NotNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.emptyList();
        }

        @Override
        public boolean check(@NonNull UltralightCard card) {
            return card.getPage(4).getData().get(0) == 0x13;
        }

        @Override
        public TransitData parseTransitData(@NonNull UltralightCard ultralightCard) {
            return new ClipperUltralightTransitData(ultralightCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull UltralightCard card) {
            return new TransitIdentity(NAME, Long.toString(getSerial(card)));
        }
    };

    @Override
    public String getSerialNumber() {
        return Long.toString(mSerial);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mSerial);
	    dest.writeInt(mBaseDate);
	    dest.writeList(mTrips);
	    dest.writeInt(mType);
	    mSub.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    private ClipperUltralightTransitData(Parcel p) {
        mSerial = p.readLong();
	    mBaseDate = p.readInt();
	    mTrips = p.readArrayList(ClipperUltralightTrip.class.getClassLoader());
	    mType = p.readInt();
	    mSub = new ClipperUltralightSubscription(p);
    }

    private ClipperUltralightTransitData(UltralightCard card) {
        mSerial = getSerial(card);
        ImmutableByteArray page0 = card.getPage(4).getData();
        ImmutableByteArray page1 = card.getPage(5).getData();
	    mBaseDate = page1.byteArrayToInt(2, 2);
	    mType = page0.get(1) & 0xff;
	    int product = page1.byteArrayToInt(0, 2);
	    mTrips = new ArrayList<>();
        ClipperUltralightTrip trLast = null;
	    for (int offset : new int[]{6,11}) {
            ImmutableByteArray trData = card.readPages(offset, 5);
            if (trData.isAllZero())
                continue;
            ClipperUltralightTrip tr = new ClipperUltralightTrip(trData, mBaseDate);
            if (trLast == null || tr.isSeqGreater(trLast))
                trLast = tr;
            if (!tr.isHidden())
                mTrips.add(tr);
        }
        int tripsRemaining = -1, transferExpiry = 0;
        if (trLast != null) {
            tripsRemaining = trLast.getTripsRemaining();
            transferExpiry = trLast.getTransferExpiry();
        }
        mSub = new ClipperUltralightSubscription(product, tripsRemaining, transferExpiry, mBaseDate);
    }

    @Override
    public List<ClipperUltralightTrip> getTrips() {
        return mTrips;
    }

    private static long getSerial(UltralightCard card) {
	    ImmutableByteArray otp = card.getPage(3).getData();
        return otp.byteArrayToLong(0, 4);
    }

    @Override
    public List<Subscription> getSubscriptions() {
        return Collections.singletonList(mSub);
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = new ArrayList<>();
        switch (mType) {
            case 0x04:
                items.add(new ListItem(R.string.ticket_type, R.string.clipper_ticket_type_adult));
                break;
            case 0x44:
                items.add(new ListItem(R.string.ticket_type, R.string.clipper_ticket_type_senior));
                break;
            case 0x84:
                items.add(new ListItem(R.string.ticket_type, R.string.clipper_ticket_type_rtc));
                break;
            case 0xc4:
                items.add(new ListItem(R.string.ticket_type, R.string.clipper_ticket_type_youth));
                break;
            default:
                items.add(new ListItem(R.string.ticket_type, Integer.toHexString(mType)));
                break;
        }

        return items;
    }
}
