/*
 * AdelaideTransaction.java
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

package au.id.micolous.metrodroid.transit.adelaide;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedString;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;

public class AdelaideTransaction extends En1545Transaction implements Parcelable {
    // Intercode but with local time instead of UTC
    private static final En1545Field TRIP_FIELDS = new En1545Container(
            En1545FixedInteger.date(EVENT),
            En1545FixedInteger.timeLocal(EVENT),
            new En1545Bitmap(
                    new En1545FixedInteger(EVENT_DISPLAY_DATA, 8),
                    new En1545FixedInteger(EVENT_NETWORK_ID, 24),
                    new En1545FixedInteger(EVENT_CODE, 8),
                    new En1545FixedInteger(EVENT_RESULT, 8),
                    new En1545FixedInteger(EVENT_SERVICE_PROVIDER, 8),
                    new En1545FixedInteger(EVENT_NOT_OK_COUNTER, 8),
                    new En1545FixedInteger(EVENT_SERIAL_NUMBER, 24),
                    new En1545FixedInteger(EVENT_DESTINATION, 16),
                    new En1545FixedInteger(EVENT_LOCATION_ID, 16),
                    new En1545FixedInteger(EVENT_LOCATION_GATE, 8),
                    new En1545FixedInteger(EVENT_DEVICE, 16),
                    new En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
                    new En1545FixedInteger(EVENT_ROUTE_VARIANT, 8),
                    new En1545FixedInteger(EVENT_JOURNEY_RUN, 16),
                    new En1545FixedInteger(EVENT_VEHICLE_ID, 16),
                    new En1545FixedInteger(EVENT_VEHICULE_CLASS, 8),
                    new En1545FixedInteger(EVENT_LOCATION_TYPE, 5),
                    new En1545FixedString(EVENT_EMPLOYEE, 240),
                    new En1545FixedInteger(EVENT_LOCATION_REFERENCE, 16),
                    new En1545FixedInteger(EVENT_JOURNEY_INTERCHANGES, 8),
                    new En1545FixedInteger(EVENT_PERIOD_JOURNEYS, 16),
                    new En1545FixedInteger(EVENT_TOTAL_JOURNEYS, 16),
                    new En1545FixedInteger(EVENT_JOURNEY_DISTANCE, 16),
                    new En1545FixedInteger(EVENT_PRICE_AMOUNT, 16),
                    new En1545FixedInteger(EVENT_PRICE_UNIT, 16),
                    new En1545FixedInteger(EVENT_CONTRACT_POINTER, 5),
                    new En1545FixedInteger(EVENT_AUTHENTICATOR, 16),
                    new En1545Bitmap(
                            En1545FixedInteger.date(EVENT_FIRST_STAMP),
                            En1545FixedInteger.timeLocal(EVENT_FIRST_STAMP),
                            new En1545FixedInteger(EVENT_DATA_SIMULATION, 1),
                            new En1545FixedInteger(EVENT_DATA_TRIP, 2),
                            new En1545FixedInteger(EVENT_DATA_ROUTE_DIRECTION, 2)
                    )
            )
    );

    public AdelaideTransaction(byte[] data) {
        super(data, TRIP_FIELDS);
    }

    private AdelaideTransaction(Parcel in) {
        super(in);
    }

    public static final Creator<AdelaideTransaction> CREATOR = new Creator<AdelaideTransaction>() {
        @Override
        public AdelaideTransaction createFromParcel(Parcel in) {
            return new AdelaideTransaction(in);
        }

        @Override
        public AdelaideTransaction[] newArray(int size) {
            return new AdelaideTransaction[size];
        }
    };

    @Override
    protected En1545Lookup getLookup() {
        return AdelaideLookup.getInstance();
    }

    protected boolean isRejected() {
        // The tap-on was rejected (insufficient funds).
        // Successful events don't set EVENT_RESULT.
        return mParsed.getIntOrZero(EVENT_RESULT) == 2;
    }
}