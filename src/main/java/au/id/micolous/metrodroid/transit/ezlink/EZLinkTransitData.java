/*
 * EZLinkTransitData.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
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

package au.id.micolous.metrodroid.transit.ezlink;

import android.os.Parcel;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.cepas.CEPASCard;
import au.id.micolous.metrodroid.card.cepas.CEPASTransaction;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

import java.util.List;

public class EZLinkTransitData extends TransitData {
    public static final Creator<EZLinkTransitData> CREATOR = new Creator<EZLinkTransitData>() {
        public EZLinkTransitData createFromParcel(Parcel parcel) {
            return new EZLinkTransitData(parcel);
        }

        public EZLinkTransitData[] newArray(int size) {
            return new EZLinkTransitData[size];
        }
    };
    private static final String EZLINK_STR = "ezlink";

    private final String mSerialNumber;
    private final double mBalance;
    private final EZLinkTrip[] mTrips;

    public EZLinkTransitData(Parcel parcel) {
        mSerialNumber = parcel.readString();
        mBalance = parcel.readDouble();

        mTrips = new EZLinkTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, EZLinkTrip.CREATOR);
    }

    public EZLinkTransitData(Card card) {
        CEPASCard cepasCard = (CEPASCard) card;
        mSerialNumber = Utils.getHexString(cepasCard.getPurse(3).getCAN(), "<Error>");
        mBalance = cepasCard.getPurse(3).getPurseBalance();
        mTrips = parseTrips(cepasCard);
    }

    private static String getCardIssuer(String canNo) {
        int issuerId = Integer.parseInt(canNo.substring(0, 3));
        switch (issuerId) {
            case 100:
                return "EZ-Link";
            case 111:
                return "NETS";
            default:
                return "CEPAS";
        }
    }

    public static Station getStation(String code) {
        if (code.length() != 3)
            return Station.unknown(code);
        return StationTableReader.getStation(EZLINK_STR, Utils.byteArrayToInt(Utils.stringToByteArray(code)), code);
    }

    public static boolean check(Card card) {
        if (card instanceof CEPASCard) {
            CEPASCard cepasCard = (CEPASCard) card;
            return cepasCard.getHistory(3) != null
                    && cepasCard.getHistory(3).isValid()
                    && cepasCard.getPurse(3) != null
                    && cepasCard.getPurse(3).isValid();
        }

        return false;
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        String canNo = Utils.getHexString(((CEPASCard) card).getPurse(3).getCAN(), "<Error>");
        return new TransitIdentity(getCardIssuer(canNo), canNo);
    }

    @Override
    public String getCardName() {
        return getCardIssuer(mSerialNumber);
    }

    @Override
    @Nullable
    public TransitCurrency getBalance() {
        // This is stored in cents of SGD
        return new TransitCurrency((int) mBalance, "SGD");
    }


    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    private EZLinkTrip[] parseTrips(CEPASCard card) {
        List<CEPASTransaction> transactions = card.getHistory(3).getTransactions();
        if (transactions != null) {
            EZLinkTrip[] trips = new EZLinkTrip[transactions.size()];

            for (int i = 0; i < trips.length; i++)
                trips[i] = new EZLinkTrip(transactions.get(i), getCardName());

            return trips;
        }
        return new EZLinkTrip[0];
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
        parcel.writeDouble(mBalance);

        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
    }

}
