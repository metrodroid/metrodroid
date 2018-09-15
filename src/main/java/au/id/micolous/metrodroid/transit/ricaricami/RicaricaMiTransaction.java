/*
 * RicaricaMiTransaction.java
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

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;
import au.id.micolous.metrodroid.util.Utils;

public class RicaricaMiTransaction extends En1545Transaction {
    private static final En1545Field TRIP_FIELDS = new En1545Container(
            En1545FixedInteger.date("Event"),
            En1545FixedInteger.timeLocal("Event"),
            new En1545FixedInteger("UnknownA", 32),
            new En1545FixedInteger("TransactionType", 2),
            new En1545FixedInteger("UnknownB", 5),
            new En1545FixedInteger("EventLocationId", 12),
            new En1545FixedInteger("UnknownC", 2),
            new En1545FixedInteger("EventRouteNumber", 10),
            new En1545FixedInteger("UnknownD", 13),
            new En1545FixedInteger("EventVehiculeId", 13),
            new En1545FixedInteger("TransportType", 4),
            new En1545FixedInteger("UnknownE", 20),
            new En1545FixedInteger("EventContractPointer", 5),
            new En1545FixedInteger("UnknownF", 16),
            En1545FixedInteger.timeLocal("EventFirstStamp"),
            new En1545FixedInteger("EventFirstLocationId", 12),
            new En1545FixedInteger("UnknownG", 3),
            new En1545FixedInteger("TransactionCounter", 4),
            new En1545FixedInteger("UnknownH", 3),
            new En1545FixedHex("UnknownI", 64)
    );

    public RicaricaMiTransaction(byte[] tripData) {
        super(tripData, TRIP_FIELDS);
    }

    private RicaricaMiTransaction(Parcel in) {
        super(in);
    }

    @Override
    protected int getTransport() {
        return mParsed.getIntOrZero("TransportType");
    }

    private static final int TRANSACTION_TAP_ON = 1;
    private static final int TRANSACTION_TAP_SOLO = 2;
    private static final int TRANSACTION_TAP_OFF = 3;

    private int getTransactionType() {
        return mParsed.getIntOrZero("TransactionType");
    }

    @Override
    protected boolean isTapOff() {
        return getTransactionType() == TRANSACTION_TAP_OFF;
    }

    @Override
    protected boolean isTapOn() {
        return getTransactionType() == TRANSACTION_TAP_ON;
    }

    @Override
    public Trip.Mode getMode() {
        switch (getTransport()) {
            case RicaricaMiLookup.TRANSPORT_METRO:
                return Trip.Mode.METRO;
            case RicaricaMiLookup.TRANSPORT_TROLLEYBUS:
                return Trip.Mode.BUS;
            case RicaricaMiLookup.TRANSPORT_TRAM:
                return Trip.Mode.TRAM;
        }
        return Trip.Mode.OTHER;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        // TODO: Is there an agency field?
        switch (getTransport()) {
            case RicaricaMiLookup.TRANSPORT_METRO:
                return Utils.localizeString(R.string.mode_metro);
            case RicaricaMiLookup.TRANSPORT_TROLLEYBUS:
                return Utils.localizeString(R.string.ricaricami_trolleybus_short);
            case RicaricaMiLookup.TRANSPORT_TRAM:
                return Utils.localizeString(R.string.mode_tram);
        }
        return Utils.localizeString(R.string.unknown_format, getTransport());
    }

    public static final Creator<RicaricaMiTransaction> CREATOR = new Creator<RicaricaMiTransaction>() {
        @Override
        public RicaricaMiTransaction createFromParcel(Parcel in) {
            return new RicaricaMiTransaction(in);
        }

        @Override
        public RicaricaMiTransaction[] newArray(int size) {
            return new RicaricaMiTransaction[size];
        }
    };

    @Override
    protected En1545Lookup getLookup() {
        return RicaricaMiLookup.getInstance();
    }

    @Override
    protected boolean isSameTrip(En1545Transaction other) {
        return (getTransport() == RicaricaMiLookup.TRANSPORT_METRO
                && (other instanceof RicaricaMiTransaction)
                && ((RicaricaMiTransaction) other).getTransport() == RicaricaMiLookup.TRANSPORT_METRO)
                || super.isSameTrip(other);
    }

}
