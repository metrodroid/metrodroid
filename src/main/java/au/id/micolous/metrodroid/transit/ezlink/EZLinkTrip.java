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

package au.id.micolous.metrodroid.transit.ezlink;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.cepas.CEPASTransaction;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class EZLinkTrip extends Trip {
    public static final Creator<EZLinkTrip> CREATOR = new Creator<EZLinkTrip>() {
        public EZLinkTrip createFromParcel(Parcel parcel) {
            return new EZLinkTrip(parcel);
        }

        public EZLinkTrip[] newArray(int size) {
            return new EZLinkTrip[size];
        }
    };
    private final CEPASTransaction mTransaction;
    private final String mCardName;

    public EZLinkTrip(CEPASTransaction transaction, String cardName) {
        mTransaction = transaction;
        mCardName = cardName;
    }

    EZLinkTrip(Parcel parcel) {
        mTransaction = parcel.readParcelable(CEPASTransaction.class.getClassLoader());
        mCardName = parcel.readString();
    }

    @Override
    public Calendar getStartTimestamp() {
        return mTransaction.getTimestamp();
    }

    @Override
    public String getAgencyName() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS
                || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            return "BUS";
        }
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION
                || mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP
                || mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE) {
            return mCardName;
        }
        if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL) {
            return "POS";
        }
        return "SMRT";
    }

    @Override
    public String getShortAgencyName() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS
                || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            return "BUS";
        }
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION
                || mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP
                || mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE) {
            if (mCardName.equals("EZ-Link")) return "EZ";
            else return mCardName;
        }
        if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL) {
            return "POS";
        }
        return "SMRT";
    }

    @Override
    public String getRouteName() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS) {
            if (mTransaction.getUserData().startsWith("SVC"))
                return Utils.localizeString(R.string.ez_bus_number,
                        mTransaction.getUserData().substring(3, 7).replace(" ", ""));
            return Utils.localizeString(R.string.unknown_format, mTransaction.getUserData());
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND)
            return Utils.localizeString(R.string.ez_bus_refund);
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.MRT)
            return Utils.localizeString(R.string.ez_mrt);
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP)
            return Utils.localizeString(R.string.ez_topup);
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
            return Utils.localizeString(R.string.ez_first_use);
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL)
            return Utils.localizeString(R.string.ez_retail_purchase);
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE)
            return Utils.localizeString(R.string.ez_service_charge);
        return Utils.localizeString(R.string.unknown_format, mTransaction.getType().toString());
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
            return null;
        return new TransitCurrency(mTransaction.getAmount(), "SGD");
    }

    @Override
    public Station getStartStation() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
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
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
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
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS
                || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND)
            return Mode.BUS;
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.MRT)
            return Mode.METRO;
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP)
            return Mode.TICKET_MACHINE;
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL
                || mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE)
            return Mode.POS;
        return Mode.OTHER;
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(mTransaction, flags);
    }

    public int describeContents() {
        return 0;
    }
}
