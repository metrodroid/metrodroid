/*
 * RicaricaMiTransitData.java
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

package au.id.micolous.metrodroid.transit.ricaricami;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;
import au.id.micolous.metrodroid.transit.en1545.En1545TransitData;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public class RicaricaMiTransitData extends En1545TransitData {
    private static final int RICARICA_MI_ID = 0x0221;
    private static final String NAME = "RicaricaMi";
    private final String mSerial;
    private final List<TransactionTrip> mTrips;
    private final List<En1545Subscription> mSubscriptions;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setLocation(R.string.location_milan)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setPreview()
            .build();

    private static final En1545Field BLOCK_1_0_FIELDS = new En1545Container(
            new En1545FixedInteger(ENV_UNKNOWN_A, 9),
            En1545FixedInteger.BCDdate(HOLDER_BIRTH_DATE),
            new En1545FixedHex(ENV_UNKNOWN_B, 47),
            En1545FixedInteger.date(ENV_APPLICATION_VALIDITY_END),
            new En1545FixedInteger(ENV_UNKNOWN_C, 26)
    );
    private static final En1545Field BLOCK_1_1_FIELDS = new En1545Container(
            new En1545FixedHex(ENV_UNKNOWN_D, 64),
            En1545FixedInteger.date(ENV_APPLICATION_ISSUE),
            new En1545FixedHex(ENV_UNKNOWN_E, 49)
    );

    private RicaricaMiTransitData(ClassicCard card) {
        mSerial = getSerial(card);
        ClassicSector sector1 = card.getSector(1);
        mTicketEnvParsed.append(sector1.getBlock(0).getData(), BLOCK_1_0_FIELDS);
        mTicketEnvParsed.append(sector1.getBlock(1).getData(), BLOCK_1_1_FIELDS);

        List<En1545Transaction> trips = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            int base = 0xa * 3 + 2 + i * 2;
            ImmutableByteArray tripData = card.getSector(base / 3).getBlock(base % 3).getData();
            tripData = tripData.plus(
                    card.getSector((base + 1) / 3).getBlock((base + 1) % 3).getData());
            if (Utils.isAllZero(tripData)) {
                continue;
            }
            trips.add(new RicaricaMiTransaction(tripData));
        }
        mTrips = TransactionTrip.merge(trips);
        mSubscriptions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ClassicSector sec = card.getSector(i+6);
            if (Utils.isAllZero(sec.getBlock(0).getData())
                    && Utils.isAllZero(sec.getBlock(1).getData())
                    && Utils.isAllZero(sec.getBlock(2).getData()))
                continue;
            ImmutableByteArray[] subData = {
                    sec.getBlock(0).getData(),
                    sec.getBlock(1).getData()
            };
            int sel = selectSubData(subData[0], subData[1]);
            mSubscriptions.add(new RicaricaMiSubscription(subData[sel],
                    card.getSector(i+2).getBlock(sel).getData()));
        }
        // TODO: check following. It might have more to do with subscription type
        // than slot
        ClassicSector sec = card.getSector(9);
        ImmutableByteArray[] subData = {
                sec.getBlock(1).getData(),
                sec.getBlock(2).getData()
        };
        if (!Utils.isAllZero(subData[0]) || !Utils.isAllZero(subData[1])) {
            int sel = selectSubData(subData[0], subData[1]);
            mSubscriptions.add(new RicaricaMiSubscription(subData[sel],
                    card.getSector(5).getBlock(1).getData()));
        }
    }

    private static int selectSubData(ImmutableByteArray subData0, ImmutableByteArray subData1) {
        int date0 = Utils.getBitsFromBuffer(subData0, 6, 14);
        int date1 = Utils.getBitsFromBuffer(subData1, 6, 14);

        if (date0 > date1)
            return 0;
        if (date0 < date1)
            return 1;

        int tapno0 = Utils.getBitsFromBuffer(subData0, 0, 6);
        int tapno1 = Utils.getBitsFromBuffer(subData1, 0, 6);

        if (tapno0 > tapno1)
            return 0;
        if (tapno0 < tapno1)
            return 1;

        if (Utils.isAllZero(subData1))
            return 0;
        if (Utils.isAllZero(subData0))
            return 1;

        if (Utils.getHexString(subData0).compareToIgnoreCase(Utils.getHexString(subData1)) > 0)
            return 0;
        return 1;
    }

    @SuppressWarnings("unchecked")
    private RicaricaMiTransitData(Parcel in) {
        super(in);
        mSerial = in.readString();
        mTrips = in.readArrayList(RicaricaMiTransaction.class.getClassLoader());
        mSubscriptions = in.readArrayList(RicaricaMiSubscription.class.getClassLoader());
    }

    @Override
    protected En1545Lookup getLookup() {
        return RicaricaMiLookup.getInstance();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mSerial);
        dest.writeList(mTrips);
        dest.writeList(mSubscriptions);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public List<TransactionTrip> getTrips() {
        return mTrips;
    }

    @Override
    public List<En1545Subscription> getSubscriptions() {
        return mSubscriptions;
    }

    public static final Creator<RicaricaMiTransitData> CREATOR = new Creator<RicaricaMiTransitData>() {
        @Override
        public RicaricaMiTransitData createFromParcel(Parcel in) {
            return new RicaricaMiTransitData(in);
        }

        @Override
        public RicaricaMiTransitData[] newArray(int size) {
            return new RicaricaMiTransitData[size];
        }
    };

    public static final ClassicCardTransitFactory FACTORY = new ClassicCardTransitFactory() {
        @Override
        public boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
            for (int i = 1; i < 3; i++) {
                ImmutableByteArray block = sectors.get(0).getBlock(i).getData();
                for (int j = (i == 1 ? 1 : 0); j < 8; j++)
                    if (Utils.byteArrayToInt(block, j * 2, 2) != RICARICA_MI_ID)
                        return false;
            }
            return true;
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return new TransitIdentity(NAME, getSerial(card));
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new RicaricaMiTransitData(classicCard);
        }

        @Override
        public int getEarlySectors() {
            return 1;
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    private static String getSerial(ClassicCard card) {
        ImmutableByteArray block2 = card.getSector(2).getBlock(2).getData();
        /* This is really weird but the following bit is 1 in my dump so it's not 45,32 unless
        * last bit needs to be inverted. To know either way we need more dumps.  */
        // TODO: check this
        return Integer.toString(Utils.getBitsFromBuffer(block2, 44, 32)*2);
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @Override
    public String getCardName() {
        return NAME;
    }
}
