/*
 * TroikaTransitData.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.troika;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Spanned;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Troika cards.
 */

public class TroikaTransitData extends TransitData {

    public static final String NAME = "Troika";
    public static final String LONG_NAME = "Troika transit card";
    private static final TimeZone TZ = TimeZone.getTimeZone("Europe/Moscow");
    public static final Parcelable.Creator<TroikaTransitData> CREATOR = new Parcelable.Creator<TroikaTransitData>() {
        public TroikaTransitData createFromParcel(Parcel parcel) {
            return new TroikaTransitData(parcel);
        }

        public TroikaTransitData[] newArray(int size) {
            return new TroikaTransitData[size];
        }
    };

    private static final String TAG = "TroikaTransitData";

    private long mSerialNumber;
    private int mBalance;
    private int mExpiryDays;

    @Nullable
    @Override
    public Integer getBalance() {
        return Integer.valueOf(mBalance);
    }

    @Override
    public Spanned formatCurrencyString(int amount, boolean isBalance) {
        return Utils.formatCurrencyString(amount, isBalance, "RUB");
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerialNumber);
    }

    public static long getSerial(ClassicSector sector) {
        byte[] b = sector.getBlock(0).getData();
        return ((long) Utils.getBitsFromBuffer(b,20, 32)) & 0xffffffffL;
    }

    private static String formatSerial(long sn) {
        DecimalFormat myFormatter = new DecimalFormat("0000000000");
        return myFormatter.format(sn);
    }

    private static int getBalance(ClassicSector sector) {
        byte[] b = sector.getBlock(1).getData();
        return Utils.getBitsFromBuffer(b,60, 22);
    }

    private static int getExpiryDays(ClassicSector sector) {
        byte[] b = sector.getBlock(0).getData();
        return Utils.getBitsFromBuffer(b,61, 16);
    }

    private static String formatDate(int days) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.set(1992, 1, 2, 12, 00);
        g.add(GregorianCalendar.DAY_OF_YEAR, days);

        DateFormat df = DateFormat.getDateInstance();
        return df.format(g.getTime());
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new ListItem(R.string.troika_expires_on, formatDate(mExpiryDays)));

        return items;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeLong(mSerialNumber);
        dest.writeInt(mBalance);
        dest.writeInt(mExpiryDays);
    }

    @SuppressWarnings("UnusedDeclaration")
    public TroikaTransitData(Parcel p) {
        mSerialNumber = p.readLong();
        mBalance = p.readInt();
        mExpiryDays = p.readInt();
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return new TransitIdentity("Troika", "" + getSerial(card.getSector(8)));
    }

    public TroikaTransitData(ClassicCard card) {
        ClassicSector sector8 = card.getSector(8);
        mSerialNumber = getSerial(sector8);
        mBalance = getBalance(sector8);
        mExpiryDays = getExpiryDays(sector8);
    }

    static public boolean check(ClassicCard card) {
        return Arrays.equals(ClassicCard.TROIKA_SECTOR_8_KEY, card.getSector(8).getKey());
    }
}
