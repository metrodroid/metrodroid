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

package au.id.micolous.metrodroid.transit.ezlinkcompat;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.List;

import au.id.micolous.metrodroid.card.cepascompat.CEPASCard;
import au.id.micolous.metrodroid.card.cepascompat.CEPASCompatTransaction;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTrip;
import au.id.micolous.metrodroid.util.Utils;

// This is only to read old dumps
public class EZLinkCompatTransitData extends TransitData {
    public static final Creator<EZLinkCompatTransitData> CREATOR = new Creator<EZLinkCompatTransitData>() {
        public EZLinkCompatTransitData createFromParcel(Parcel parcel) {
            return new EZLinkCompatTransitData(parcel);
        }

        public EZLinkCompatTransitData[] newArray(int size) {
            return new EZLinkCompatTransitData[size];
        }
    };

    private final String mSerialNumber;
    private final double mBalance;
    private final EZLinkCompatTrip[] mTrips;

    public EZLinkCompatTransitData(Parcel parcel) {
        mSerialNumber = parcel.readString();
        mBalance = parcel.readDouble();

        mTrips = new EZLinkCompatTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, EZLinkCompatTrip.CREATOR);
    }

    public EZLinkCompatTransitData(CEPASCard cepasCard) {
        mSerialNumber = Utils.getHexString(cepasCard.getPurse(3).getCAN(), "<Error>");
        mBalance = cepasCard.getPurse(3).getPurseBalance();
        mTrips = parseTrips(cepasCard);
    }

    public static TransitIdentity parseTransitIdentity(CEPASCard card) {
        String canNo = Utils.getHexString(card.getPurse(3).getCAN(), "<Error>");
        return new TransitIdentity(EZLinkTransitData.getCardIssuer(canNo), canNo);
    }

    @Override
    public String getCardName() {
        return EZLinkTransitData.getCardIssuer(mSerialNumber);
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

    private EZLinkCompatTrip[] parseTrips(CEPASCard card) {
        List<CEPASCompatTransaction> transactions = card.getHistory(3).getTransactions();
        if (transactions != null) {
            EZLinkCompatTrip[] trips = new EZLinkCompatTrip[transactions.size()];

            for (int i = 0; i < trips.length; i++)
                trips[i] = new EZLinkCompatTrip(transactions.get(i), getCardName());

            return trips;
        }
        return new EZLinkCompatTrip[0];
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
        parcel.writeDouble(mBalance);

        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
    }

}
