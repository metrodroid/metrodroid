/*
 * IntercodeTrip.java
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

package au.id.micolous.metrodroid.transit.intercode;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedString;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

class IntercodeTransaction extends En1545Transaction {
    private final int mNetworkId;

    public static final Parcelable.Creator<IntercodeTransaction> CREATOR = new Parcelable.Creator<IntercodeTransaction>() {
        @NonNull
        public IntercodeTransaction createFromParcel(Parcel parcel) {
            return new IntercodeTransaction(parcel);
        }

        @NonNull
        public IntercodeTransaction[] newArray(int size) {
            return new IntercodeTransaction[size];
        }
    };

    private static final En1545Field tripFields = new En1545Container(
            En1545FixedInteger.date(EVENT),
            En1545FixedInteger.time(EVENT),
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
                            En1545FixedInteger.time(EVENT_FIRST_STAMP),
                            new En1545FixedInteger(EVENT_DATA_SIMULATION, 1),
                            new En1545FixedInteger(EVENT_DATA_TRIP, 2),
                            new En1545FixedInteger(EVENT_DATA_ROUTE_DIRECTION, 2)
                    )
            )
    );

    IntercodeTransaction(ImmutableByteArray data, int networkId) {
        super(data, tripFields);

        Integer nid = mParsed.getInt(EVENT_NETWORK_ID);
        if (nid != null)
            mNetworkId = nid;
        else
            mNetworkId = networkId;
    }

    @Override
    protected En1545Lookup getLookup() {
        return IntercodeTransitData.getLookup(mNetworkId);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mNetworkId);
    }

    private IntercodeTransaction(Parcel parcel) {
        super(parcel);
        mNetworkId = parcel.readInt();
    }
}
