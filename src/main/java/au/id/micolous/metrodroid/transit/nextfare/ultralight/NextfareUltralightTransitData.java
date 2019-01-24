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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.NumberUtils;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

/* Based on reference at http://www.lenrek.net/experiments/compass-tickets/. */
public abstract class NextfareUltralightTransitData extends TransitData {
    private final int mProductCode;
    private final long mSerial;
    private final byte mType;
    private final int mBaseDate;
    private final int mMachineCode;
    private final List<TransactionTrip> mTrips;
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
        dest.writeList(mTrips);
    }

    protected NextfareUltralightTransitData(Parcel p) {
        mSerial = p.readLong();
        mType = p.readByte();
        mBaseDate = p.readInt();
        mProductCode = p.readInt();
        mMachineCode = p.readInt();
        mBalance = p.readInt();
        mExpiry = p.readInt();
        //noinspection unchecked
        mTrips = p.readArrayList(NextfareUltralightTransaction.class.getClassLoader());
    }

    protected NextfareUltralightTransitData(UltralightCard card) {
        mSerial = getSerial(card);
        ImmutableByteArray page0 = card.getPage(4).getData();
        ImmutableByteArray page1 = card.getPage(5).getData();
        ImmutableByteArray page3 = card.getPage(7).getData();
        mType = page0.get(1);
        int lowerBaseDate = page0.get(3) & 0xff;
        int upperBaseDate = page1.get(0) & 0xff;
        mBaseDate = (upperBaseDate << 8) | lowerBaseDate;
        mProductCode = page1.get(2) & 0x7f;
        mMachineCode = page3.byteArrayToIntReversed(0, 2);
        List <NextfareUltralightTransaction> transactions = new ArrayList<>();
        if (isTransactionValid(card, 8)) {
            transactions.add(makeTransaction(card, 8, mBaseDate));
        }
        if (isTransactionValid(card, 12)) {
            transactions.add(makeTransaction(card, 12, mBaseDate));
        }
        NextfareUltralightTransaction trLater = null;
        for (NextfareUltralightTransaction tr : transactions)
            if (trLater == null || tr.isSeqNoGreater(trLater))
                trLater = tr;
        if (trLater != null) {
            mExpiry = trLater.getExpiry();
            mBalance = trLater.getBalance();
        } else {
            mExpiry = 0;
            mBalance = 0;
        }
        mTrips = TransactionTrip.merge(transactions);
    }

    private static boolean isTransactionValid(UltralightCard card, int startPage) {
        return !card.readPages(startPage, 3).isAllZero();
    }

    protected abstract NextfareUltralightTransaction makeTransaction(UltralightCard card,
                                                                     int startPage, int baseDate);

    protected static long getSerial(UltralightCard card) {
        ImmutableByteArray manufData0 = card.getPage(0). getData();
        ImmutableByteArray manufData1 = card.getPage(1). getData();
        long uid = (manufData0.byteArrayToLong(1, 2) << 32)
                | (manufData1.byteArrayToLong(0, 4));
        long serial = uid + 1000000000000000L;
        int luhn = NumberUtils.INSTANCE.calculateLuhn(Long.toString(serial));
        return serial * 10 + luhn;
    }

    protected static String formatSerial(long serial) {
        return NumberUtils.INSTANCE.formatNumber(serial, " ", 4, 4, 4, 4, 4);
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = new ArrayList<>();
        if (mType == 8)
            items.add(new ListItem(R.string.ticket_type, R.string.compass_ticket_type_concession));
        else
            items.add(new ListItem(R.string.ticket_type, R.string.compass_ticket_type_regular));

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
    public List<TransactionTrip> getTrips() {
        return mTrips;
    }

    @NonNull
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
