/*
 * MobibTrip.java
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

package au.id.micolous.metrodroid.transit.mobib;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;
import au.id.micolous.metrodroid.util.StationTableReader;

class MobibTransaction extends En1545Transaction {
    private static final int TRAM = 0x16;
    private static final int BUS = 0xf;

    public static final Creator<MobibTransaction> CREATOR = new Creator<MobibTransaction>() {
        @NonNull
        public MobibTransaction createFromParcel(Parcel parcel) {
            return new MobibTransaction(parcel);
        }

        @NonNull
        public MobibTransaction[] newArray(int size) {
            return new MobibTransaction[size];
        }
    };

    private static final String EVENT_LOCATION_ID_BUS = "EventLocationIdBus";
    private static final En1545Field FIELDS = new En1545Container(
            new En1545FixedInteger(EVENT_UNKNOWN_A, 6),
            En1545FixedInteger.date(EVENT),
            En1545FixedInteger.time(EVENT),
            new En1545FixedInteger(EVENT_UNKNOWN_B, 21),
            new En1545FixedInteger(EVENT_PASSENGER_COUNT, 5),
            new En1545FixedInteger(EVENT_UNKNOWN_C, 14),
            new En1545FixedInteger(EVENT_LOCATION_ID_BUS, 12), // Curious
            new En1545FixedInteger(EVENT_UNKNOWN_D, 9),
            new En1545FixedInteger(EVENT_ROUTE_NUMBER, 7), // curious
            new En1545FixedInteger(EVENT_SERVICE_PROVIDER, 5), // Curious
            new En1545FixedInteger(EVENT_LOCATION_ID, 17), // Curious
            new En1545FixedInteger(EVENT_UNKNOWN_E, 10),
            new En1545FixedInteger(EVENT_UNKNOWN_F, 7),
            new En1545FixedInteger(EVENT_SERIAL_NUMBER, 24),
            new En1545FixedInteger("EventTransferNumber", 24),
            En1545FixedInteger.date(EVENT_FIRST_STAMP),
            En1545FixedInteger.time(EVENT_FIRST_STAMP),
            new En1545FixedInteger(EVENT_UNKNOWN_G, 21)
    );

    public MobibTransaction(byte[] data) {
        super(data, FIELDS);
    }

    @Nullable
    @Override
    public Station getStation() {
        int agency = mParsed.getIntOrZero(EVENT_SERVICE_PROVIDER);
        if (agency == TRAM)
            return null;
        if (agency == BUS)
            return StationTableReader.getStation(MobibLookup.MOBIB_STR,
                    mParsed.getIntOrZero(EVENT_ROUTE_NUMBER) << 13
                    | mParsed.getIntOrZero(EVENT_LOCATION_ID_BUS) | (agency << 22));
        return super.getStation();
    }

    @Override
    protected En1545Lookup getLookup() {
        return MobibLookup.getInstance();
    }

    private MobibTransaction(Parcel parcel) {
        super(parcel);
    }
}
