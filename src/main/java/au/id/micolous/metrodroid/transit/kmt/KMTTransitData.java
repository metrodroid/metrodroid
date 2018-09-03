/*
 * KMTTransitData.java
 *
 * Copyright 2018 Bondan Sumbodo <sybond@gmail.com>
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

package au.id.micolous.metrodroid.transit.kmt;

import android.os.Parcel;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.felica.FelicaService;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class KMTTransitData extends TransitData {
    // defines
    public static final String NAME = "Kartu Multi Trip";
    private static final int SYSTEMCODE_KMT = 0x90b7;
    private static final int FELICA_SERVICE_KMT_ID = 0x300B;
    private static final int FELICA_SERVICE_KMT_BALANCE = 0x1017;
    private static final int FELICA_SERVICE_KMT_HISTORY = 0x200F;
    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Jakarta");
    public static final long KMT_EPOCH;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.kmt_card)
            .setName(KMTTransitData.NAME)
            .setLocation(R.string.location_jakarta)
            .setCardType(CardType.FeliCa)
            .setExtraNote(R.string.kmt_extra_note)
            .build();

    static {
        GregorianCalendar epoch = new GregorianCalendar(TIME_ZONE);
        epoch.set(2000, 0, 1, 7, 0, 0);
        KMT_EPOCH = epoch.getTimeInMillis();
    }

    public static final Creator<KMTTransitData> CREATOR = new Creator<KMTTransitData>() {
        public KMTTransitData createFromParcel(Parcel parcel) {
            return new KMTTransitData(parcel);
        }

        public KMTTransitData[] newArray(int size) {
            return new KMTTransitData[size];
        }
    };
    private KMTTrip[] mTrips;
    private String mSerialNumber;
    private int mCurrentBalance;

    private KMTTransitData(Parcel parcel) {
        mTrips = new KMTTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, KMTTrip.CREATOR);
        mCurrentBalance = parcel.readInt();
        mSerialNumber = parcel.readString();
    }

    public KMTTransitData(FelicaCard card) {
        FelicaService serviceID = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_ID);
        List<FelicaBlock> blocksID = serviceID.getBlocks();
        FelicaBlock blockID = blocksID.get(0);
        byte[] dataID = blockID.getData();
        mSerialNumber = new String(dataID);

        FelicaService serviceBalance = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_BALANCE);
        if (serviceBalance != null) {
            List<FelicaBlock> blocksBalance = serviceBalance.getBlocks();
            FelicaBlock blockBalance = blocksBalance.get(0);
            byte[] dataBalance = blockBalance.getData();
            mCurrentBalance = Utils.byteArrayToIntReversed(dataBalance, 0, 4);
        }

        FelicaService serviceHistory = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_HISTORY);
        List<KMTTrip> trips = new ArrayList<>();
        List<FelicaBlock> blocks = serviceHistory.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            FelicaBlock block = blocks.get(i);
            if (block.getData()[0] != 0) {
                KMTTrip trip = new KMTTrip(block);
                trips.add(trip);
            }
        }
        mTrips = trips.toArray(new KMTTrip[trips.size()]);
    }

    public static boolean check(FelicaCard card) {
        return (card.getSystem(SYSTEMCODE_KMT) != null);
    }

    public static boolean earlyCheck(int[] systemCodes) {
        return ArrayUtils.contains(systemCodes, SYSTEMCODE_KMT);
    }

    public static TransitIdentity parseTransitIdentity(FelicaCard card) {
        FelicaService serviceID = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_ID);
        String serialNumber = "-";
        if (serviceID != null) {
            serialNumber = new String(serviceID.getBlocks().get(0).getData());
        }
        return new TransitIdentity(NAME, serialNumber);
    }

    @Override
    @Nullable
    public TransitCurrency getBalance() {
        return TransitCurrency.IDR(mCurrentBalance);
    }

    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
        parcel.writeInt(mCurrentBalance);
        parcel.writeString(mSerialNumber);
    }
}

