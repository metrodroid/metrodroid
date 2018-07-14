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
import android.util.Log;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Troika cards.
 */

public class TroikaTransitData extends TransitData {

    public static final String NAME = "Troika";

    // We don't want to actually include these keys in the program, so include a hashed version of
    // this key.
    private static final String KEY_SALT = "troika";
    // md5sum of Salt + Common Key + Salt, used on sector 8.
    private static final String KEY_DIGEST = "6621dd07ad2954ffe49739ad88e744cf";

    private static final TimeZone TZ = TimeZone.getTimeZone("Europe/Moscow");

    private static final long TROIKA_EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(1992, Calendar.FEBRUARY, 2, 12, 0, 0);

        TROIKA_EPOCH = epoch.getTimeInMillis();
    }

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

    /**
     * Balance of the card, in kopeyka (0.01 RUB).
     */
    private int mBalance;

    /**
     * Expiry date of the card, in days since the TROIKA_EPOCH.
     */
    private int mExpiryDays;

    @Nullable
    @Override
    public Integer getBalance() {
        return mBalance;
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

    private static Calendar convertDate(int days) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(TROIKA_EPOCH);
        g.add(GregorianCalendar.DAY_OF_YEAR, days);
        return g;
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();
        items.add(new ListItem(R.string.card_expiry_date,
                Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(convertDate(mExpiryDays)))));
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
        return new TransitIdentity(NAME, "" + getSerial(card.getSector(8)));
    }

    public TroikaTransitData(ClassicCard card) {
        ClassicSector sector8 = card.getSector(8);
        mSerialNumber = getSerial(sector8);
        mBalance = getBalance(sector8);
        mExpiryDays = getExpiryDays(sector8);
    }

    public static boolean check(ClassicCard card) {
        byte[] key = card.getSector(8).getKey();
        if (key == null || key.length != 6) {
            // We don't have key data, bail out.
            return false;
        }

        Log.d(TAG, "Checking for Troika key...");
        return Utils.checkKeyHash(key, KEY_SALT, KEY_DIGEST) >= 0;
    }
}
