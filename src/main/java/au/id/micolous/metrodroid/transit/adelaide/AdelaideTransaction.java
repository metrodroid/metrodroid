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
                    new En1545FixedInteger("EventDisplayData", 8),
                    new En1545FixedInteger(EVENT_NETWORK_ID, 24),
                    new En1545FixedInteger(EVENT_CODE, 8),
                    new En1545FixedInteger(EVENT_RESULT, 8),
                    new En1545FixedInteger(EVENT_SERVICE_PROVIDER, 8),
                    new En1545FixedInteger("EventNotOkCounter", 8),
                    new En1545FixedInteger("EventSerialNumber", 24),
                    new En1545FixedInteger("EventDestination", 16),
                    new En1545FixedInteger(EVENT_LOCATION_ID, 16),
                    new En1545FixedInteger("EventLocationGate", 8),
                    new En1545FixedInteger("EventDevice", 16),
                    new En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
                    new En1545FixedInteger(EVENT_ROUTE_VARIANT, 8),
                    new En1545FixedInteger("EventJourneyRun", 16),
                    new En1545FixedInteger(EVENT_VEHICLE_ID, 16),
                    new En1545FixedInteger("EventVehiculeClass", 8),
                    new En1545FixedInteger("EventLocationType", 5),
                    new En1545FixedString("EventEmployee", 240),
                    new En1545FixedInteger("EventLocationReference", 16),
                    new En1545FixedInteger("EventJourneyInterchanges", 8),
                    new En1545FixedInteger("EventPeriodJourneys", 16),
                    new En1545FixedInteger("EventTotalJourneys", 16),
                    new En1545FixedInteger("EventJourneyDistance", 16),
                    new En1545FixedInteger(EVENT_PRICE_AMOUNT, 16),
                    new En1545FixedInteger("EventPriceUnit", 16),
                    new En1545FixedInteger(EVENT_CONTRACT_POINTER, 5),
                    new En1545FixedInteger(EVENT_AUTHENTICATOR, 16),
                    new En1545Bitmap(
                            En1545FixedInteger.date(EVENT_FIRST_STAMP),
                            En1545FixedInteger.timeLocal(EVENT_FIRST_STAMP),
                            new En1545FixedInteger("EventDataSimulation", 1),
                            new En1545FixedInteger("EventDataTrip", 2),
                            new En1545FixedInteger("EventDataRouteDirection", 2)
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
}