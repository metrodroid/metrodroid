/*
 * TroikaUltralightTransitData.java
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
package au.id.micolous.metrodroid.transit.compass;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

/* Based on reference at http://www.lenrek.net/experiments/compass-tickets/. */
public class CompassUltralightTransitData extends TransitData {

    public static final Parcelable.Creator<CompassUltralightTransitData> CREATOR = new Parcelable.Creator<CompassUltralightTransitData>() {
        public CompassUltralightTransitData createFromParcel(Parcel parcel) {
            return new CompassUltralightTransitData(parcel);
        }

        public CompassUltralightTransitData[] newArray(int size) {
            return new CompassUltralightTransitData[size];
        }
    };
    public static final String NAME = "Compass Ultralight";
    private final int mProductCode;
    private final long mSerial;
    private final byte mType;
    private final int mBaseDate;
    private final int mMachineCode;
    private final List<CompassUltralightTrip> mTrips;
    private final int mExpiry;
    private final int mBalance;

    private static final TimeZone TZ = TimeZone.getTimeZone("America/Vancouver");

    public static boolean check(UltralightCard card) {
        try {
            int head = Utils.byteArrayToInt(card.getPage(4).getData(), 0, 3);
            return head == 0x0a0400 || head == 0x0a0800;
        } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
            // If that sector number is too high, then it's not for us.
            return false;
        }
    }

    @Nullable
    @Override
    public TransitCurrency getBalance() {
        return TransitCurrency.CAD(mBalance);
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mSerial);
        dest.writeByte(mType);
        dest.writeInt(mBaseDate);
        dest.writeInt(mProductCode);
        dest.writeInt(mMachineCode);
        dest.writeInt(mBalance);
        dest.writeInt(mExpiry);
        dest.writeParcelableArray(mTrips.toArray(new CompassUltralightTrip[0]), flags);
    }

    private CompassUltralightTransitData(Parcel p) {
        mSerial = p.readLong();
        mType = p.readByte();
        mBaseDate = p.readInt();
        mProductCode = p.readInt();
        mMachineCode = p.readInt();
        mBalance = p.readInt();
        mExpiry = p.readInt();
        mTrips = Arrays.asList((CompassUltralightTrip[]) p.readParcelableArray(CompassUltralightTrip.class.getClassLoader()));
    }

    public CompassUltralightTransitData(UltralightCard card) {
        mSerial = getSerial(card);
        byte []page0 = card.getPage(4).getData();
        byte []page1 = card.getPage(5).getData();
        byte []page3 = card.getPage(7).getData();
        mType = page0[1];
        int lowerBaseDate = page0[3] & 0xff;
        int upperBaseDate = page1[0] & 0xff;
        mBaseDate = (upperBaseDate << 8) | lowerBaseDate;
        mProductCode = page1[2] & 0x7f;
        mMachineCode = Utils.byteArrayToIntReversed(page3, 0, 2);
        mTrips = new ArrayList<>();
        CompassUltralightTransaction trA = new CompassUltralightTransaction(card, 8, mBaseDate);
        CompassUltralightTransaction trB = new CompassUltralightTransaction(card, 12, mBaseDate);
        CompassUltralightTransaction trLater;
        if (trA.isSeqNoGreater(trB))
            trLater = trA;
        else
            trLater = trB;
        mExpiry = trLater.getExpiry();
        mBalance = trLater.getBalance();
        if (trA.isSameTripTapOut(trB))
            mTrips.add(new CompassUltralightTrip(trB, trA));
        else if (trB.isSameTripTapOut(trA))
            mTrips.add(new CompassUltralightTrip(trA, trB));
        else {
            if (trA.isTapOut())
                mTrips.add(new CompassUltralightTrip(null, trA));
            else
                mTrips.add(new CompassUltralightTrip(trA, null));
            if (trB.isTapOut())
                mTrips.add(new CompassUltralightTrip(null, trB));
            else
                mTrips.add(new CompassUltralightTrip(trB, null));
        }
    }

    private static long getSerial(UltralightCard card) {
        byte []manufData0 = card.getPage(0). getData();
        byte []manufData1 = card.getPage(1). getData();
        long uid = (Utils.byteArrayToLong(manufData0, 1, 2) << 32)
                | (Utils.byteArrayToLong(manufData1, 0, 4));
        long serial = uid + 1000000000000000L;
        int luhn = Utils.calculateLuhn(Long.toString(serial));
        return serial * 10 + luhn;
    }

    public static TransitIdentity parseTransitIdentity(UltralightCard card) {
        return new TransitIdentity(NAME, formatSerial(getSerial(card)));
    }

    private static String formatSerial(long serial) {
        StringBuilder res = new StringBuilder();
        long val = serial;
        for (int i = 0; i < 5; i++) {
            if (res.length() != 0)
                res.insert(0, " ");
            res.insert(0, String.format(Locale.ENGLISH, "%04d", val % 10000));
            val /= 10000;
        }
        return res.toString();
    }

    private static final Map<Integer, String> productCodes = new HashMap<>();

    static {
        // TODO: i18n
        productCodes.put(0x01, "DayPass");
        productCodes.put(0x02, "One Zone");
        productCodes.put(0x03, "Two Zone");
        productCodes.put(0x04, "Three Zone");
        productCodes.put(0x0f, "Four Zone WCE (one way)");
        productCodes.put(0x11, "Free Sea Island");
        productCodes.put(0x16, "Exit");
        productCodes.put(0x1e, "One Zone with YVR");
        productCodes.put(0x1f, "Two Zone with YVR");
        productCodes.put(0x20, "Three Zone with YVR");
        productCodes.put(0x21, "DayPass with YVR");
        productCodes.put(0x22, "Bulk DayPass");
        productCodes.put(0x23, "Bulk One Zone");
        productCodes.put(0x24, "Bulk Two Zone");
        productCodes.put(0x25, "Bulk Three Zone");
        productCodes.put(0x26, "Bulk One Zone");
        productCodes.put(0x27, "Bulk Two Zone");
        productCodes.put(0x28, "Bulk Three Zone");
        productCodes.put(0x29, "GradPass");
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();
        if (mType == 8)
            items.add(new ListItem(R.string.compass_ticket_type, R.string.compass_ticket_type_concession));
        else
            items.add(new ListItem(R.string.compass_ticket_type, R.string.compass_ticket_type_regular));

        if (productCodes.containsKey(mProductCode))
            items.add(new ListItem(R.string.compass_product_type, productCodes.get(mProductCode)));
        else
            items.add(new ListItem(R.string.compass_product_type, Integer.toHexString(mProductCode)));
        items.add(new ListItem(R.string.compass_machine_code, Integer.toHexString(mMachineCode)));
        items.add(new ListItem(R.string.compass_expiry_date, Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(parseDateTime(mBaseDate, -mExpiry, 0)))));
        return items;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new CompassUltralightTrip[0]);
    }

    public static Calendar parseDateTime(int baseDate, int date, int time) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.set((baseDate >> 9) + 2000,
                ((baseDate >> 5) & 0xf) - 1,
                baseDate & 0x1f, 0, 0, 0);
        g.add(Calendar.DAY_OF_YEAR, date);
        g.add(Calendar.MINUTE, time);
        return g;
    }
}
