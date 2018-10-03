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
package au.id.micolous.metrodroid.transit.nextfare.ultralight;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

/* Based on reference at http://www.lenrek.net/experiments/compass-tickets/. */
public abstract class NextfareUltralightTransitData extends TransitData {
    private final int mProductCode;
    private final long mSerial;
    private final byte mType;
    private final int mBaseDate;
    private final int mMachineCode;
    private final List<NextfareUltralightTrip> mTrips;
    private final int mExpiry;
    private final int mBalance;

    protected abstract TransitCurrency makeCurrency(int val);

    @Nullable
    @Override
    public TransitBalance getBalance() {
        return new TransitBalanceStored(
                makeCurrency(mBalance),
                parseDateTime(getTimeZone(), mBaseDate, mExpiry, 0));
    }

    protected abstract TimeZone getTimeZone();

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
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
        dest.writeParcelableArray(mTrips.toArray(new NextfareUltralightTrip[0]), flags);
    }

    protected NextfareUltralightTransitData(Parcel p) {
        mSerial = p.readLong();
        mType = p.readByte();
        mBaseDate = p.readInt();
        mProductCode = p.readInt();
        mMachineCode = p.readInt();
        mBalance = p.readInt();
        mExpiry = p.readInt();
        mTrips = Arrays.asList((NextfareUltralightTrip[]) p.readParcelableArray(NextfareUltralightTrip.class.getClassLoader()));
    }

    protected NextfareUltralightTransitData(UltralightCard card) {
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
        NextfareUltralightTransaction trA = null, trB = null;
        if (isTransactionValid(card, 8))
            trA = makeTransaction(card, 8, mBaseDate);
        if (isTransactionValid(card, 12))
            trB = makeTransaction(card, 12, mBaseDate);
        NextfareUltralightTransaction trLater;
        if (trB == null || trA != null && trA.isSeqNoGreater(trB))
            trLater = trA;
        else
            trLater = trB;
        if (trLater != null) {
            mExpiry = trLater.getExpiry();
            mBalance = trLater.getBalance();
        } else {
            mExpiry = 0;
            mBalance = 0;
        }
        if (trA != null && trB != null && trA.isSameTripTapOut(trB))
            mTrips.add(new NextfareUltralightTrip(trB, trA));
        else if (trA != null && trB != null && trB.isSameTripTapOut(trA))
            mTrips.add(new NextfareUltralightTrip(trA, trB));
        else {
            if (trA != null && trA.isTapOut())
                mTrips.add(new NextfareUltralightTrip(null, trA));
            else if (trA != null)
                mTrips.add(new NextfareUltralightTrip(trA, null));
            if (trB != null && trB.isTapOut())
                mTrips.add(new NextfareUltralightTrip(null, trB));
            else if (trB != null)
                mTrips.add(new NextfareUltralightTrip(trB, null));
        }
    }

    private static boolean isTransactionValid(UltralightCard card, int startPage) {
        for (int i = 0; i < 3; i++)
            if (!Utils.isAllZero(card.getPage(startPage + i).getData()))
                return true;
        return false;
    }

    protected abstract NextfareUltralightTransaction makeTransaction(UltralightCard card,
                                                                     int startPage, int baseDate);

    protected static long getSerial(UltralightCard card) {
        byte []manufData0 = card.getPage(0). getData();
        byte []manufData1 = card.getPage(1). getData();
        long uid = (Utils.byteArrayToLong(manufData0, 1, 2) << 32)
                | (Utils.byteArrayToLong(manufData1, 0, 4));
        long serial = uid + 1000000000000000L;
        int luhn = Utils.calculateLuhn(Long.toString(serial));
        return serial * 10 + luhn;
    }

    protected static String formatSerial(long serial) {
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

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();
        if (mType == 8)
            items.add(new ListItem(R.string.compass_ticket_type, R.string.compass_ticket_type_concession));
        else
            items.add(new ListItem(R.string.compass_ticket_type, R.string.compass_ticket_type_regular));

        String productName = getProductName(mProductCode);
        if (productName != null)
            items.add(new ListItem(R.string.compass_product_type, productName));
        else
            items.add(new ListItem(R.string.compass_product_type, Integer.toHexString(mProductCode)));
        items.add(new ListItem(R.string.compass_machine_code, Integer.toHexString(mMachineCode)));
        return items;
    }

    protected abstract String getProductName(int productCode);

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new NextfareUltralightTrip[0]);
    }

    public static Calendar parseDateTime(TimeZone tz, int baseDate, int date, int time) {
        GregorianCalendar g = new GregorianCalendar(tz);
        g.set((baseDate >> 9) + 2000,
                ((baseDate >> 5) & 0xf) - 1,
                baseDate & 0x1f, 0, 0, 0);
        g.add(Calendar.DAY_OF_YEAR, -date);
        g.add(Calendar.MINUTE, time);
        return g;
    }
}
