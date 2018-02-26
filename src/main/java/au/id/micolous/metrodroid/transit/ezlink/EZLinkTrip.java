/*
 * EZLinkTrip.java
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

import java.util.Calendar;

import au.id.micolous.metrodroid.card.cepas.CEPASTransaction;
import au.id.micolous.metrodroid.transit.CompatTrip;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;

public class EZLinkTrip extends CompatTrip {
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
    public long getTimestamp() {
        return mTransaction.getTimestamp();
    }

    @Override
    public String getAgencyName() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS
                || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
            if (EZLinkTransitData.sbsBuses.contains(routeString))
                return "SBS";
            return "SMRT";
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
            String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
            if (EZLinkTransitData.sbsBuses.contains(routeString))
                return "SBS";
            return "SMRT";
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
        // TODO: i18n
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS) {
            if (mTransaction.getUserData().startsWith("SVC"))
                return "Bus #" + mTransaction.getUserData().substring(3, 7).replace(" ", "");
            return "(Unknown Bus Route)";
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND)
            return "Bus Refund";
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.MRT)
            return "MRT";
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP)
            return "Top-up";
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
            return "First use";
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL)
            return "Retail Purchase";
        else if (mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE)
            return "Service Charge";
        return "(Unknown Route)";
    }

    @Override
    public boolean hasFare() {
        return (mTransaction.getType() != CEPASTransaction.TransactionType.CREATION);
    }

    @Nullable
    @Override
    public Integer getFare() {
        return mTransaction.getAmount();
    }

    @Override
    public Station getStartStation() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
            return null;
        if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
            String startStationAbbr = mTransaction.getUserData().substring(0, 3);
            return EZLinkTransitData.getStation(startStationAbbr);
        }
        return null;
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
    public String getStartStationName() {
        Station startStation = getStartStation();
        if (startStation != null)
            return startStation.getStationName();
        else if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
            return mTransaction.getUserData().substring(0, 3); // extract startStationAbbr
        }
        return mTransaction.getUserData();
    }

    @Override
    public String getEndStationName() {
        Station endStation = getEndStation();
        if (endStation != null)
            return endStation.getStationName();
        else if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
            return mTransaction.getUserData().substring(4, 7); // extract endStationAbbr
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
