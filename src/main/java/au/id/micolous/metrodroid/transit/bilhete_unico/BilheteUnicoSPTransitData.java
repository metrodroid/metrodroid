/*
 * BilheteUnicoSPTransitData.java
 *
 * Copyright 2013 Marcelo Liberato <mliberato@gmail.com>
 * Copyright 2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.bilhete_unico;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

public class BilheteUnicoSPTransitData extends TransitData {
    private static final String NAME = "Bilhete Único";
    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.bilheteunicosp_card, R.drawable.bilheteunicosp_card_alpha)
            .setName(NAME)
            .setLocation(R.string.location_sao_paulo)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setExtraNote(R.string.card_note_bilhete_unico)
            .build();

    public static final Creator<BilheteUnicoSPTransitData> CREATOR = new Creator<BilheteUnicoSPTransitData>() {
        public BilheteUnicoSPTransitData createFromParcel(Parcel parcel) {
            return new BilheteUnicoSPTransitData(parcel);
        }

        public BilheteUnicoSPTransitData[] newArray(int size) {
            return new BilheteUnicoSPTransitData[size];
        }
    };
    private final int mCredit;
    private final int mTransactionCounter;
    private final int mRefillTransactionCounter;
    private final List<Trip> mTrips;
    private final int mDay2;
    private final long mSerial;

    @SuppressWarnings("unchecked")
    private BilheteUnicoSPTransitData(Parcel parcel) {
        mSerial = parcel.readLong();
        mCredit = parcel.readInt();
        mTransactionCounter = parcel.readInt();
        mRefillTransactionCounter = parcel.readInt();
        mDay2 = parcel.readInt();
        mTrips = parcel.readArrayList(BilheteUnicoSPTrip.class.getClassLoader());
    }

    private BilheteUnicoSPTransitData(ClassicCard card) {
        mSerial = getSerial(card);
        ClassicSector identitySector = card.getSector(2);
        mDay2 = identitySector.getBlock(0).getData().getBitsFromBuffer(2, 14);

        ClassicSector creditSector = card.getSector(8);
        mCredit = creditSector.getBlock(1).getData().byteArrayToIntReversed(0, 4);

        ImmutableByteArray creditBlock0 = creditSector.getBlock(0).getData();

        int lastRefillDay = creditBlock0.getBitsFromBuffer(2, 14);
        int lastRefillAmount = creditBlock0.getBitsFromBuffer(29, 11);
        mRefillTransactionCounter = creditBlock0.getBitsFromBuffer(44, 14);

        ClassicSector lastTripSector = card.getSector(3);
        if (!checkCRC16Sector(lastTripSector))
            lastTripSector = card.getSector(4);

        ImmutableByteArray tripBlock0 = lastTripSector.getBlock(0).getData();
        mTransactionCounter = tripBlock0.getBitsFromBuffer(48, 14);
        ImmutableByteArray block1 = lastTripSector.getBlock(1).getData();
        int day = block1.getBitsFromBuffer(76, 14);
        int time =  block1.getBitsFromBuffer(90, 11);
        ImmutableByteArray block2 = lastTripSector.getBlock(2).getData();
        int firstTapDay = block2.getBitsFromBuffer(2, 14);
        int firstTapTime = block2.getBitsFromBuffer(16, 11);
        int firstTapLine = block2.getBitsFromBuffer(27, 9);
        mTrips = new ArrayList<>();
        if (day != 0)
            mTrips.add(new BilheteUnicoSPTrip(lastTripSector));
        if (firstTapDay != day || firstTapTime != time)
            mTrips.add(new BilheteUnicoSPFirstTap(firstTapDay, firstTapTime, firstTapLine));
        if (lastRefillDay != 0)
            mTrips.add(new BilheteUnicoSPRefill(lastRefillDay, lastRefillAmount));
    }

    private static String formatSerial(long val) {
        return String.format(Locale.ENGLISH, "%02d0 %09d",
                val >> 36, (val >> 4) & 0xffffffffL);
    }

    private static long getSerial(ClassicCard card) {
        return card.getSector(2).getBlock(0).getData().byteArrayToLong(3, 5);
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> li = new ArrayList<>();
        li.add(new ListItem(R.string.trip_counter,
                Integer.toString(mTransactionCounter)));
        li.add(new ListItem(R.string.refill_counter,
                Integer.toString(mRefillTransactionCounter)));
        // It looks like issue date but on some dumps it's after the trips, so it can't be.
        li.add(new ListItem(new SpannableString("Date 1"),
                Utils.longDateFormat(BilheteUnicoSPTrip.parseTimestamp(mDay2, 0))));
        return li;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mSerial);
        parcel.writeInt(mCredit);
        parcel.writeInt(mTransactionCounter);
        parcel.writeInt(mRefillTransactionCounter);
        parcel.writeInt(mDay2);
        parcel.writeList(mTrips);
    }

    @Override
    @Nullable
    public TransitCurrency getBalance() {
        return TransitCurrency.BRL(mCredit);
    }

    @Override
    public List<Trip> getTrips() {
        return mTrips;
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean checkValueBlock(ClassicBlock block, int addr) {
        ImmutableByteArray data = block.getData();
        return data.byteArrayToInt(0, 4) == ~data.byteArrayToInt(4, 4)
                && data.byteArrayToInt(0, 4) == data.byteArrayToInt(8, 4)
	        && data.get(12) == addr && data.get(14) == addr && data.get(13) == data.get(15)
                && (data.get(13) & 0xff) == (~addr & 0xff);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean checkCRC16Sector(ClassicSector s) {
        List<ClassicBlock> blocks = s.getBlocks();
        int crc = 0;
        for (ClassicBlock b : blocks.subList(0, blocks.size()-1))
            crc = Utils.calculateCRC16IBM(b.getData().getDataCopy(), crc);
        return crc == 0;
    }

    public static final ClassicCardTransitFactory FACTORY = new ClassicCardTransitFactory () {
        @Override
        public boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
            // Normally both sectors are identical but occasionally one of them might get corrupted,
            // so tolerate one failure
            if (!checkCRC16Sector(sectors.get(3))
                    && !checkCRC16Sector(sectors.get(4)))
                return false;
            for (int sectoridx = 5; sectoridx <= 8; sectoridx++) {
                int addr = sectoridx * 4 + 1;
                ClassicSector sector = sectors.get(sectoridx);
                if (!checkValueBlock(sector.getBlock(1), addr))
                    return false;
                if (!checkValueBlock(sector.getBlock(2), addr))
                    return false;
            }
            return true;
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return new TransitIdentity(NAME, formatSerial(getSerial(card)));
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new BilheteUnicoSPTransitData(classicCard);
        }

        @Override
        public int earlySectors() {
            return 9;
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };
}
