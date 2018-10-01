/*
 * OpusTrip.java
 *
 * Copyright 2018 Etienne Dubeau
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

package au.id.micolous.metrodroid.transit.opus;

import android.os.Parcel;

import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;

class OpusTransaction extends En1545Transaction {
    public static final Creator<OpusTransaction> CREATOR = new Creator<OpusTransaction>() {
        public OpusTransaction createFromParcel(Parcel parcel) {
            return new OpusTransaction(parcel);
        }

        public OpusTransaction[] newArray(int size) {
            return new OpusTransaction[size];
        }
    };

    private static final En1545Field tripFields = new En1545Container(
            En1545FixedInteger.date(EVENT),
            En1545FixedInteger.timeLocal(EVENT),
            new En1545FixedInteger("UnknownX", 19), // Possibly part of following bitmap
            new En1545Bitmap(
                    new En1545FixedInteger(EVENT_UNKNOWN_A, 8),
                    new En1545FixedInteger(EVENT_UNKNOWN_B, 8),
                    new En1545FixedInteger(EVENT_SERVICE_PROVIDER, 8),
                    new En1545FixedInteger(EVENT_UNKNOWN_C, 16),
                    new En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
                    // How 32 bits are split among next 2 fields is unclear
                    new En1545FixedInteger(EVENT_UNKNOWN_D, 16),
                    new En1545FixedInteger(EVENT_UNKNOWN_E, 16),
                    new En1545FixedInteger(EVENT_CONTRACT_POINTER, 5),
                    new En1545Bitmap(
                            En1545FixedInteger.date(EVENT_FIRST_STAMP),
                            En1545FixedInteger.time(EVENT_FIRST_STAMP),
                            new En1545FixedInteger("EventDataSimulation", 1),
                            new En1545FixedInteger(EVENT_UNKNOWN_F, 4),
                            new En1545FixedInteger(EVENT_UNKNOWN_G, 4),
                            new En1545FixedInteger(EVENT_UNKNOWN_H, 4),
                            new En1545FixedInteger(EVENT_UNKNOWN_I, 4)
                    )
            )
    );

    public OpusTransaction(byte[] data) {
        super(data, tripFields);
    }

    @Override
    protected En1545Lookup getLookup() {
        return OpusLookup.getInstance();
    }

    private OpusTransaction(Parcel parcel) {
        super(parcel);
    }
}
