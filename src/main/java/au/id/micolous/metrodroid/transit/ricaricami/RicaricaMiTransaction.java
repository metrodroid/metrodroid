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
import android.support.annotation.NonNull;

import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

public class RicaricaMiTransaction extends En1545Transaction {
    private static final String TRANSPORT_TYPE = "TransportType";
    private static final String TRANSACTION_TYPE = "TransactionType";
    private static final String TRANSACTION_COUNTER = "TransactionCounter";
    private static final En1545Field TRIP_FIELDS = new En1545Container(
            En1545FixedInteger.date(EVENT),
            En1545FixedInteger.timeLocal(EVENT),
            new En1545FixedInteger(EVENT_UNKNOWN_A, 32),
            new En1545FixedInteger(TRANSACTION_TYPE, 2),
            new En1545FixedInteger(EVENT_UNKNOWN_B, 5),
            new En1545FixedInteger(EVENT_LOCATION_ID, 12),
            new En1545FixedInteger(EVENT_UNKNOWN_C, 2),
            new En1545FixedInteger(EVENT_ROUTE_NUMBER, 10),
            new En1545FixedInteger(EVENT_UNKNOWN_D, 13),
            new En1545FixedInteger(EVENT_VEHICLE_ID, 13),
            new En1545FixedInteger(TRANSPORT_TYPE, 4),
            new En1545FixedInteger(EVENT_UNKNOWN_E, 20),
            new En1545FixedInteger(EVENT_CONTRACT_POINTER, 5),
            new En1545FixedInteger(EVENT_UNKNOWN_F, 16),
            En1545FixedInteger.timeLocal(EVENT_FIRST_STAMP),
            new En1545FixedInteger(EVENT_FIRST_LOCATION_ID, 12),
            new En1545FixedInteger(EVENT_UNKNOWN_G, 3),
            new En1545FixedInteger(TRANSACTION_COUNTER, 4),
            new En1545FixedInteger(EVENT_UNKNOWN_H, 3),
            new En1545FixedHex(EVENT_UNKNOWN_I, 64)
    );

    public RicaricaMiTransaction(ImmutableByteArray tripData) {
        super(tripData, TRIP_FIELDS);
    }

    private RicaricaMiTransaction(Parcel in) {
        super(in);
    }

    @Override
    protected int getTransport() {
        return mParsed.getIntOrZero(TRANSPORT_TYPE);
    }

    private static final int TRANSACTION_TAP_ON = 1;
    private static final int TRANSACTION_TAP_SOLO = 2;
    private static final int TRANSACTION_TAP_OFF = 3;

    private int getTransactionType() {
        return mParsed.getIntOrZero(TRANSACTION_TYPE);
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
                return Trip.Mode.TROLLEYBUS;
            case RicaricaMiLookup.TRANSPORT_TRAM:
                return Trip.Mode.TRAM;
        }
        return Trip.Mode.OTHER;
    }

    @Override
    public String getAgencyName(boolean isShort) {
	return isShort ? "ATM" : "Azienda Trasporti Milanesi";
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
    protected boolean isSameTrip(@NonNull Transaction other) {
        return (getTransport() == RicaricaMiLookup.TRANSPORT_METRO
                && (other instanceof RicaricaMiTransaction)
                && ((RicaricaMiTransaction) other).getTransport() == RicaricaMiLookup.TRANSPORT_METRO)
                || super.isSameTrip(other);
    }

}
