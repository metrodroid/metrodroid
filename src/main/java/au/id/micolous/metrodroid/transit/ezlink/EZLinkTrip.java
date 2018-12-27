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

import org.jetbrains.annotations.NonNls;

import java.util.Calendar;

import au.id.micolous.farebot.R;
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
    private final @NonNls String mCardName;

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
    public String getAgencyName(boolean isShort) {
        return getAgencyName(mTransaction.getType(), mCardName, isShort);
    }

    @Override
    public String getRouteName() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS) {
            if (mTransaction.getUserData().startsWith("SVC") || mTransaction.getUserData().startsWith("BUS"))
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
        return TransitCurrency.SGD(-mTransaction.getAmount());
    }

    @SuppressWarnings("MagicCharacter")
    @Override
    public Station getStartStation() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS
                && (mTransaction.getUserData().startsWith("SVC")
                || mTransaction.getUserData().startsWith("BUS")))
            return null;
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
            return Station.nameOnly(mTransaction.getUserData());
        if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
            String startStationAbbr = mTransaction.getUserData().substring(0, 3);
            return EZLinkTransitData.getStation(startStationAbbr);
        }
        return Station.nameOnly(mTransaction.getUserData());
    }

    @SuppressWarnings("MagicCharacter")
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

    public static Mode getMode(CEPASTransaction.TransactionType type) {
        if (type == CEPASTransaction.TransactionType.BUS
                || type == CEPASTransaction.TransactionType.BUS_REFUND)
            return Mode.BUS;
        else if (type == CEPASTransaction.TransactionType.MRT)
            return Mode.METRO;
        else if (type == CEPASTransaction.TransactionType.TOP_UP)
            return Mode.TICKET_MACHINE;
        else if (type == CEPASTransaction.TransactionType.RETAIL
                || type == CEPASTransaction.TransactionType.SERVICE)
            return Mode.POS;
        return Mode.OTHER;
    }


    @Override
    public Mode getMode() {
        return getMode(mTransaction.getType());
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(mTransaction, flags);
    }

    public int describeContents() {
        return 0;
    }

    public static String getAgencyName(CEPASTransaction.TransactionType type, @NonNls String cardName, boolean isShort) {
        if (type == CEPASTransaction.TransactionType.BUS
                || type == CEPASTransaction.TransactionType.BUS_REFUND) {
            return "BUS";
        }
        if (type == CEPASTransaction.TransactionType.CREATION
                || type == CEPASTransaction.TransactionType.TOP_UP
                || type == CEPASTransaction.TransactionType.SERVICE) {
            if (isShort && cardName.equals("EZ-Link")) return "EZ";
            return cardName;
        }
        if (type == CEPASTransaction.TransactionType.RETAIL) {
            return "POS";
        }
        return "SMRT";
    }

    public static String getRouteName(CEPASTransaction.TransactionType type, String userData) {
        if (type == CEPASTransaction.TransactionType.BUS) {
            if (userData.startsWith("SVC") || userData.startsWith("BUS"))
                return Utils.localizeString(R.string.ez_bus_number,
                        userData.substring(3, 7).replace(" ", ""));
            return Utils.localizeString(R.string.unknown_format, userData);
        } else if (type == CEPASTransaction.TransactionType.BUS_REFUND)
            return Utils.localizeString(R.string.ez_bus_refund);
        else if (type == CEPASTransaction.TransactionType.MRT)
            return Utils.localizeString(R.string.ez_mrt);
        else if (type == CEPASTransaction.TransactionType.TOP_UP)
            return Utils.localizeString(R.string.ez_topup);
        else if (type == CEPASTransaction.TransactionType.CREATION)
            return Utils.localizeString(R.string.ez_first_use);
        else if (type == CEPASTransaction.TransactionType.RETAIL)
            return Utils.localizeString(R.string.ez_retail_purchase);
        else if (type == CEPASTransaction.TransactionType.SERVICE)
            return Utils.localizeString(R.string.ez_service_charge);
        return Utils.localizeString(R.string.unknown_format, type.toString());
    }
}
