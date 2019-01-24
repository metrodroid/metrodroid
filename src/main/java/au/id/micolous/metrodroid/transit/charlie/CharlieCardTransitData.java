/*
 * CharlieCardTransitData.java
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

package au.id.micolous.metrodroid.transit.charlie;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public class CharlieCardTransitData extends TransitData {

    private final static String NAME = "CharlieCard";
    private final long mSerial;
    private final long mSecondSerial;
    private final int mBalance;
    private final int mStartDate;
    private final List<CharlieCardTrip> mTrips;
    private static final long CHARLIE_EPOCH;
    private static final TimeZone TZ = TimeZone.getTimeZone("America/Boston");
    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setLocation(R.string.location_boston)
            .setCardType(CardType.MifareClassic)
            .setImageId(R.drawable.charlie_card, R.drawable.iso7810_id1_alpha)
            .setKeysRequired()
            .setPreview()
            .build();

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(2003, Calendar.JANUARY, 1, 0, 0, 0);

        CHARLIE_EPOCH = epoch.getTimeInMillis();
    }

    private CharlieCardTransitData(ClassicCard card) {
        mSerial = getSerial(card);
        mSecondSerial = card.getSector(8).getBlock(0).getData().byteArrayToLong(0, 4);
        ClassicSector sector2 = card.getSector(2);
        ClassicSector sector3 = card.getSector(3);
        ClassicSector balanceSector;
        if (sector2.getBlock(0).getData().getBitsFromBuffer(81, 16)
            > sector3.getBlock(0).getData().getBitsFromBuffer(81, 16))
            balanceSector = sector2;
        else
            balanceSector = sector3;
        mBalance = getPrice(balanceSector.getBlock(1).getData(), 5);
        mStartDate = balanceSector.getBlock(0).getData().byteArrayToInt(6, 3);
        mTrips = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            ClassicBlock block = card.getSector(6 + (i / 6)).getBlock((i / 2) % 3);
            if (block.getData().byteArrayToInt(7 * (i % 2), 4) == 0)
                continue;
            mTrips.add(new CharlieCardTrip(block.getData(), 7 * (i % 2)));
        }
    }

    public static int getPrice(ImmutableByteArray data, int off) {
        int val = data.byteArrayToInt(off, 2);
        if ((val & 0x8000) != 0) {
            val = -(val & 0x7fff);
        }
        return val / 2;
    }

    private CharlieCardTransitData(Parcel in) {
        mSerial = in.readLong();
        mSecondSerial = in.readLong();
        mBalance = in.readInt();
        mStartDate = in.readInt();
        //noinspection unchecked
        mTrips = in.readArrayList(CharlieCardTrip.class.getClassLoader());
    }

    public static Calendar parseTimestamp(int timestamp) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(CHARLIE_EPOCH);
        g.add(GregorianCalendar.MINUTE, timestamp);
        return g;
    }

    @Nullable
    @Override
    protected TransitBalance getBalance() {
        Calendar start = parseTimestamp(mStartDate);

        // After 2011, all cards expire 10 years after issue.
        // Cards were first issued in 2006, and would expire after 5 years, and had no printed
        // expiry date.
        // However, currently (2018), all of these have expired anyway.
        Calendar expiry = (Calendar) start.clone();
        expiry.add(Calendar.YEAR, 11);
        expiry.add(Calendar.DAY_OF_YEAR, -1);

        // Find the last trip taken on the card.
        Calendar lastTrip = null;
        for (CharlieCardTrip t : mTrips) {
            if (lastTrip == null || t.getStartTimestamp().getTimeInMillis() > lastTrip.getTimeInMillis()) {
                lastTrip = t.getStartTimestamp();
            }
        }

        if (lastTrip != null) {
            // Cards not used for 2 years will also expire
            lastTrip = (Calendar) lastTrip.clone();
            lastTrip.add(Calendar.YEAR, 2);

            if (lastTrip.getTimeInMillis() < expiry.getTimeInMillis()) {
                expiry = lastTrip;
            }
        }
        return new TransitBalanceStored(TransitCurrency.USD(mBalance), null, start, expiry);
    }

    @Override
    public List<CharlieCardTrip> getTrips() {
        return mTrips;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mSerial);
        dest.writeLong(mSecondSerial);
        dest.writeInt(mBalance);
        dest.writeInt(mStartDate);
        dest.writeList(mTrips);
    }

    public static final Creator<CharlieCardTransitData> CREATOR = new Creator<CharlieCardTransitData>() {
        @Override
        public CharlieCardTransitData createFromParcel(Parcel in) {
            return new CharlieCardTransitData(in);
        }

        @Override
        public CharlieCardTransitData[] newArray(int size) {
            return new CharlieCardTransitData[size];
        }
    };

    private static long getSerial(ClassicCard card) {
        return card.getSector(0).getBlock(0).getData().byteArrayToLong(0, 4);
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    @NonNls
    private static String formatSerial(long serial) {
        return "5-" + Long.toString(serial);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    public static final ClassicCardTransitFactory FACTORY = new ClassicCardTransitFactory() {
        @Override
        public boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
            ClassicSector sector0 = sectors.get(0);
            ImmutableByteArray b = sector0.getBlock(1).getData();
            if (!b.sliceOffLen(2, 14).contentEquals(new byte[]{
                    0x04, 0x10, 0x04, 0x10, 0x04, 0x10,
                    0x04, 0x10, 0x04, 0x10, 0x04, 0x10, 0x04, 0x10
            }))
                return false;
            ClassicSector sector1 = sectors.get(1);
            if (sector1 instanceof UnauthorizedClassicSector)
                return true;
            b = sector1.getBlock(0).getData();
            return b.sliceOffLen(0, 6).contentEquals(new byte[]{
                    0x04, 0x10, 0x23, 0x45, 0x66, 0x77
            });
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return new TransitIdentity(NAME, formatSerial(getSerial(card)));
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new CharlieCardTransitData(classicCard);
        }

        @Override
        public int getEarlySectors() {
            return 2;
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    @Override
    public List<ListItem> getInfo() {
        if (mSecondSerial == 0 || mSecondSerial == 0xffffffffL)
            return null;
        return Collections.singletonList(new ListItem(R.string.charlie_2nd_card_number,
                String.format(Locale.ENGLISH, "A%010d", mSecondSerial)));
    }
}
