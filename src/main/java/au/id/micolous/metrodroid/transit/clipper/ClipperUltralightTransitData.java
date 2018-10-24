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

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

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

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setLocation(R.string.location_san_francisco)
            .setCardType(CardType.MifareUltralight)
            .build();

    private final long mSerial;
    private final int mBaseDate;
    private final List<ClipperUltralightTrip> mTrips;
    private final int mType;
    private final ClipperUltralightSubscription mSub;

    public static boolean check(UltralightCard card) {
        try {
            byte[] head = card.getPage(4).getData();
            return head[0] == 0x13;
        } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
            // If that sector number is too high, then it's not for us.
            return false;
        }
    }

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

    @SuppressWarnings("unchecked")
    private ClipperUltralightTransitData(Parcel p) {
        mSerial = p.readLong();
	    mBaseDate = p.readInt();
	    mTrips = p.readArrayList(ClipperUltralightTrip.class.getClassLoader());
	    mType = p.readInt();
	    mSub = new ClipperUltralightSubscription(p);
    }

    private static byte[] getTransaction(UltralightCard card, int startPage) {
        byte[] ret = new byte[]{};
        for (int i = 0; i < 5; i++)
            ret = Utils.concatByteArrays(ret, card.getPage(startPage + i).getData());
        return ret;
    }

    public ClipperUltralightTransitData(UltralightCard card) {
        mSerial = getSerial(card);
        byte []page0 = card.getPage(4).getData();
	    byte []page1 = card.getPage(5).getData();
	    mBaseDate = Utils.byteArrayToInt(page1, 2, 2);
	    mType = page0[1] & 0xff;
	    int product = Utils.byteArrayToInt(page1, 0, 2);
	    mTrips = new ArrayList<>();
        ClipperUltralightTrip trLast = null;
	    for (int offset : new int[]{6,11}) {
            byte[] trData = getTransaction(card, offset);
            if (Utils.isAllZero(trData))
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
        mSub = new ClipperUltralightSubscription(product, tripsRemaining, transferExpiry);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new Trip[0]);
    }

    private static long getSerial(UltralightCard card) {
	    byte []otp = card.getPage(3).getData();
        return Utils.byteArrayToLong(otp, 0, 4);
    }

    public static TransitIdentity parseTransitIdentity(UltralightCard card) {
        return new TransitIdentity(NAME, Long.toString(getSerial(card)));
    }

    @Override
    public Subscription[] getSubscriptions() {
        return new Subscription[]{mSub};
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();
        items.add(new ListItem(R.string.expiry_date, Utils.longDateFormat(
                ClipperTransitData.clipperTimestampToCalendar(mBaseDate * 86400L))));
        items.add(new ListItem(R.string.issue_date, Utils.longDateFormat(
                ClipperTransitData.clipperTimestampToCalendar((mBaseDate - 89) * 86400L))));
        switch (mType) {
            case 0x04:
                items.add(new ListItem(R.string.clipper_ticket_type, R.string.clipper_ticket_type_adult));
                break;
            case 0x44:
                items.add(new ListItem(R.string.clipper_ticket_type, R.string.clipper_ticket_type_senior));
                break;
            case 0x84:
                items.add(new ListItem(R.string.clipper_ticket_type, R.string.clipper_ticket_type_rtc));
                break;
            case 0xc4:
                items.add(new ListItem(R.string.clipper_ticket_type, R.string.clipper_ticket_type_youth));
                break;
            default:
                items.add(new ListItem(R.string.clipper_ticket_type, Integer.toHexString(mType)));
                break;
        }

        return items;
    }
}
