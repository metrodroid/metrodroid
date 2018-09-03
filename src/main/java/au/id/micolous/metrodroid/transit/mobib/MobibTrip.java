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

import java.util.Calendar;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

class MobibTrip extends Trip {
    private static final int TRAM = 0x16;
    private static final int BUS = 0xf;
    private final int mDay;
    private final int mTime;
    private final int mAgency;
    private final int mRoute;
    private final int mStationId;
    private final int mPax;
    public static final String MOBIB_STR = "mobib";

    // Missing: bus direction
    // Missing: connection counter
    // Missing: first connection timestamp
    // Missing: transit flag

    public static final Creator<MobibTrip> CREATOR = new Creator<MobibTrip>() {
        @NonNull
        public MobibTrip createFromParcel(Parcel parcel) {
            return new MobibTrip(parcel);
        }

        @NonNull
        public MobibTrip[] newArray(int size) {
            return new MobibTrip[size];
        }
    };

    public MobibTrip(byte[] data) {
        mDay = Utils.getBitsFromBuffer(data, 6, 14);
        mTime = Utils.getBitsFromBuffer(data, 20, 11);

        mPax = Utils.getBitsFromBuffer(data, 52, 5);
        mAgency = Utils.getBitsFromBuffer(data, 99, 5);
        mRoute = Utils.getBitsFromBuffer(data, 92, 8);
        if (mAgency == BUS) {
            mStationId = Utils.getBitsFromBuffer(data, 71, 13)
            | (mRoute << 13);
        } else {
            mStationId = Utils.getBitsFromBuffer(data, 104, 17);
        }
    }

    @Override
    public String getRouteName() {
        String route, pax;
        switch (mAgency) {
            case BUS:
                route = Integer.toString(mRoute);
                break;
            case TRAM:
                route = null;
                break;
            default:
                route = StationTableReader.getStation(MOBIB_STR, mStationId).getLineName();
                break;
        }
        pax = Utils.localizePlural(R.plurals.mobib_pax_count, mPax, mPax);
        if (route != null)
            return String.format(Locale.ENGLISH, "%s, %s", route, pax);
        return pax;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return StationTableReader.getOperatorName(MOBIB_STR, mAgency, isShort);
    }


    @Nullable
    @Override
    public Station getStartStation() {
        if (mAgency == TRAM)
            return null;
        return StationTableReader.getStation(MOBIB_STR, mStationId | (mAgency << 22));
    }

    @Override
    public Calendar getStartTimestamp() {
        return MobibTransitData.parseTime(mDay, mTime);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return null;
    }

    @Override
    public Mode getMode() {
        return StationTableReader.getOperatorDefaultMode(MOBIB_STR, mAgency);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mDay);
        dest.writeInt(mTime);
        dest.writeInt(mAgency);
        dest.writeInt(mStationId);
        dest.writeInt(mRoute);
        dest.writeInt(mPax);
    }

    public MobibTrip(Parcel parcel) {
        mDay = parcel.readInt();
        mTime = parcel.readInt();
        mAgency = parcel.readInt();
        mStationId = parcel.readInt();
        mRoute = parcel.readInt();
        mPax = parcel.readInt();
    }
}
