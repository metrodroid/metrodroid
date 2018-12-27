/*
 * EZLinkTrip.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import java.util.Calendar;

import au.id.micolous.metrodroid.card.cepascompat.CEPASCompatTransaction;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.ezlink.CEPASTransaction;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTrip;

public class EZLinkCompatTrip extends Trip {
    public static final Creator<EZLinkCompatTrip> CREATOR = new Creator<EZLinkCompatTrip>() {
        public EZLinkCompatTrip createFromParcel(Parcel parcel) {
            return new EZLinkCompatTrip(parcel);
        }

        public EZLinkCompatTrip[] newArray(int size) {
            return new EZLinkCompatTrip[size];
        }
    };
    private final CEPASCompatTransaction mTransaction;
    private final String mCardName;

    public EZLinkCompatTrip(CEPASCompatTransaction transaction, String cardName) {
        mTransaction = transaction;
        mCardName = cardName;
    }

    private EZLinkCompatTrip(Parcel parcel) {
        mTransaction = parcel.readParcelable(CEPASCompatTransaction.class.getClassLoader());
        mCardName = parcel.readString();
    }

    @Override
    public Calendar getStartTimestamp() {
        return mTransaction.getTimestamp();
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return EZLinkTrip.getAgencyName(getType(), mCardName, isShort);
    }

    @Override
    public String getRouteName() {
        return EZLinkTrip.getRouteName(getType(), mTransaction.getUserData());
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        if (getType() == CEPASTransaction.TransactionType.CREATION)
            return null;
        return TransitCurrency.SGD(-mTransaction.getAmount());
    }

    private CEPASTransaction.TransactionType getType() {
        return CEPASTransaction.getType(mTransaction.getType());
    }

    @Override
    public Station getStartStation() {
        CEPASTransaction.TransactionType type = getType();
        if (type == CEPASTransaction.TransactionType.BUS
                && (mTransaction.getUserData().startsWith("SVC")
                || mTransaction.getUserData().startsWith("BUS")))
            return null;
        if (type == CEPASTransaction.TransactionType.CREATION)
            return Station.nameOnly(mTransaction.getUserData());
        if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
            String startStationAbbr = mTransaction.getUserData().substring(0, 3);
            return EZLinkTransitData.getStation(startStationAbbr);
        }
        return Station.nameOnly(mTransaction.getUserData());
    }

    @Override
    public Station getEndStation() {
        if (getType() == CEPASTransaction.TransactionType.CREATION)
            return null;
        if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
            String endStationAbbr = mTransaction.getUserData().substring(4, 7);
            return EZLinkTransitData.getStation(endStationAbbr);
        }
        return null;
    }

    @Override
    public Mode getMode() {
        return EZLinkTrip.getMode(CEPASTransaction.getType(mTransaction.getType()));
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(mTransaction, flags);
    }

    public int describeContents() {
        return 0;
    }
}
