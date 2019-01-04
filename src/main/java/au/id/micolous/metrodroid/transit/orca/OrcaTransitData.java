/*
 * OrcaTransitData.java
 *
 * Copyright 2011-2013 Eric Butler <eric@codebutler.com>
 * Copyright 2015 Sean CyberKitsune McClenaghan
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package au.id.micolous.metrodroid.transit.orca;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrcaTransitData extends TransitData {
    static final int AGENCY_KCM = 0x04;
    static final int AGENCY_ST = 0x07;
    static final int AGENCY_WSF = 0x08;

    public static final int APP_ID = 0x3010f2;

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.orca_card)
            .setName("ORCA")
            .setLocation(R.string.location_seattle)
            .setCardType(CardType.MifareDesfire)
            .build();

    private final int mSerialNumber;
    private final int mBalance;
    private final List<Trip> mTrips;

    public static final Creator<OrcaTransitData> CREATOR = new Creator<OrcaTransitData>() {
        public OrcaTransitData createFromParcel(Parcel parcel) {
            return new OrcaTransitData(parcel);
        }

        public OrcaTransitData[] newArray(int size) {
            return new OrcaTransitData[size];
        }
    };


    private OrcaTransitData(Parcel parcel) {
        mSerialNumber = parcel.readInt();
        mBalance = parcel.readInt();

        mTrips = new ArrayList<>();
        parcel.readList(mTrips, OrcaTransaction.class.getClassLoader());
    }

    private OrcaTransitData(DesfireCard desfireCard) {
        ImmutableByteArray data;

        try {
            data = desfireCard.getApplication(0xffffff).getFile(0x0f).getData();
            mSerialNumber = Utils.byteArrayToInt(data, 5, 3);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA serial", ex);
        }

        try {
            data = desfireCard.getApplication(APP_ID).getFile(0x04).getData();
            mBalance = Utils.byteArrayToInt(data, 41, 2);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA balance", ex);
        }

        try {
            mTrips = new ArrayList<>();
            mTrips.addAll(parseTrips(desfireCard, 2, false));
            mTrips.addAll(parseTrips(desfireCard, 3, true));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA trips", ex);
        }
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean earlyCheck(int[] appIds) {
            return ArrayUtils.contains(appIds, APP_ID);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }

        @Override
        public TransitData parseTransitData(@NonNull DesfireCard desfireCard) {
            return new OrcaTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull DesfireCard card) {
            try {
                ImmutableByteArray data = card.getApplication(0xffffff).getFile(0x0f).getData();
                return new TransitIdentity("ORCA", String.valueOf(Utils.byteArrayToInt(data, 4, 4)));
            } catch (Exception ex) {
                throw new RuntimeException("Error parsing ORCA serial", ex);
            }
        }
    };

    @Override
    public String getCardName() {
        return "ORCA";
    }

    @Override
    @Nullable
    public TransitCurrency getBalance() {
        return TransitCurrency.USD(mBalance);
    }

    @Override
    public String getSerialNumber() {
        return Integer.toString(mSerialNumber);
    }

    @Override
    public List<Trip> getTrips() {
        return mTrips;
    }

    private List <TransactionTrip> parseTrips(DesfireCard card, int fileId, boolean isTopup) {
        DesfireFile file = card.getApplication(APP_ID).getFile(fileId);
        if (!(file instanceof RecordDesfireFile))
            return new ArrayList<>();

        RecordDesfireFile recordFile = (RecordDesfireFile) file;

        OrcaTransaction[] useLog = new OrcaTransaction[recordFile.getRecords().size()];
        for (int i = 0; i < useLog.length; i++) {
            useLog[i] = new OrcaTransaction(recordFile.getRecords().get(i), isTopup);
        }
        return TransactionTrip.merge(useLog);
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mSerialNumber);
        parcel.writeInt(mBalance);

        parcel.writeList(mTrips);
    }

}
