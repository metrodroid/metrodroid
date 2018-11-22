/*
 * LisboaVivaTrip.java
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

package au.id.micolous.metrodroid.transit.lisboaviva;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;

class LisboaVivaTransaction extends En1545Transaction {
    public static final Creator<LisboaVivaTransaction> CREATOR = new Creator<LisboaVivaTransaction>() {
        public LisboaVivaTransaction createFromParcel(Parcel parcel) {
            return new LisboaVivaTransaction(parcel);
        }

        public LisboaVivaTransaction[] newArray(int size) {
            return new LisboaVivaTransaction[size];
        }
    };

    private static final String TRANSITION = "Transition";
    private static final En1545Field tripFields = new En1545Container(
            En1545FixedInteger.dateTime(EVENT),
            new En1545FixedHex(EVENT_UNKNOWN_A, 38),
            new En1545FixedInteger("ContractsUsedBitmap", 4),
            new En1545FixedHex(EVENT_UNKNOWN_B, 29),
            new En1545FixedInteger(TRANSITION, 3),
            new En1545FixedInteger(EVENT_SERVICE_PROVIDER, 5), // Curious
            new En1545FixedHex(EVENT_UNKNOWN_C, 20),
            new En1545FixedInteger(EVENT_DEVICE_ID, 16),
            new En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
            new En1545FixedInteger(EVENT_LOCATION_ID, 8),
            new En1545FixedHex(EVENT_UNKNOWN_D, 63)
    );

    @Nullable
    public Station getStation(Integer station) {
        if (station == null)
            return null;
        return getLookup().getStation(station, getAgency(), mParsed.getIntOrZero(EVENT_ROUTE_NUMBER));
    }

    protected boolean isTapOn() {
        int transition = mParsed.getIntOrZero(TRANSITION);
        return transition == 1;
    }

    protected boolean isTapOff() {
        int transition = mParsed.getIntOrZero(TRANSITION);
        return transition == 4;
    }

    LisboaVivaTransaction(byte[] data) {
        super(data, tripFields);
    }


    private LisboaVivaTransaction(Parcel parcel) {
        super(parcel);
    }

    @NonNull
    @Override
    public List<String> getRouteNames() {
        int routeNumber = mParsed.getInt(EVENT_ROUTE_NUMBER);
        if (getAgency() == LisboaVivaLookup.AGENCY_CP
                && routeNumber == LisboaVivaLookup.ROUTE_CASCAIS_SADO) {
            if (getStationId() <= 54)
                return Collections.singletonList("Cascais");
            else
                return Collections.singletonList("Sado");
        }

        return super.getRouteNames();
    }

    @Override
    protected En1545Lookup getLookup() {
        return LisboaVivaLookup.getInstance();
    }
}
