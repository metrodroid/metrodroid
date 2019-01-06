/*
 * ClipperTrip.java
 *
 * Copyright 2011 "an anonymous contributor"
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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
package au.id.micolous.metrodroid.transit.clipper;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class ClipperTrip extends Trip {
    public static final Creator<ClipperTrip> CREATOR = new Creator<ClipperTrip>() {
        public ClipperTrip createFromParcel(Parcel parcel) {
            return new ClipperTrip(parcel);
        }

        public ClipperTrip[] newArray(int size) {
            return new ClipperTrip[size];
        }
    };
    private final long mTimestamp;
    private final long mExitTimestamp;
    private final int mFare;
    private final int mAgency;
    private final int mFrom;
    private final int mTo;
    private final int mRoute;
    private final int mVehicleNum;
    private final int mTransportCode;

    private ClipperTrip(Parcel parcel) {
        mTimestamp = parcel.readLong();
        mExitTimestamp = parcel.readLong();
        mFare = parcel.readInt();
        mAgency = parcel.readInt();
        mFrom = parcel.readInt();
        mTo = parcel.readInt();
        mRoute = parcel.readInt();
        mVehicleNum = parcel.readInt();
        mTransportCode = parcel.readInt();
    }

    ClipperTrip(byte[] useData) {
        mAgency = Utils.byteArrayToInt(useData, 0x2, 2);
        mFare = Utils.byteArrayToInt(useData, 0x6, 2);
        mVehicleNum = Utils.byteArrayToInt(useData, 0xa, 2);
        mTimestamp = Utils.byteArrayToLong(useData, 0xc, 4);
        mExitTimestamp = Utils.byteArrayToLong(useData, 0x10, 4);
        mFrom = Utils.byteArrayToInt(useData, 0x14, 2);
        mTo = Utils.byteArrayToInt(useData, 0x16, 2);
        mRoute = Utils.byteArrayToInt(useData, 0x1c, 2);
        mTransportCode = Utils.byteArrayToInt(useData, 0x1e, 2);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return ClipperData.getAgencyName(mAgency, isShort);
    }

    @Override
    public Calendar getStartTimestamp() {
        return ClipperTransitData.clipperTimestampToCalendar(mTimestamp);
    }

    @Override
    public Calendar getEndTimestamp() {
        return ClipperTransitData.clipperTimestampToCalendar(mExitTimestamp);
    }

    @Override
    public String getRouteName() {
        if (mAgency == ClipperData.AGENCY_GG_FERRY) {
            return ClipperData.GG_FERRY_ROUTES.get(mRoute);
        } else {
            // Bus doesn't record line
            return null;
        }
    }

    @Nullable
    @Override
    public String getHumanReadableRouteID() {
        if (mAgency == ClipperData.AGENCY_GG_FERRY) {
            return Utils.intToHex(mRoute);
        }

        return null;
    }

    @Override
    public String getVehicleID() {
        if (mVehicleNum != 0 && mVehicleNum != 0xffff)
            return Integer.toString(mVehicleNum);
        return null;
    }

    @Override
    @Nullable
    public TransitCurrency getFare() {
        return TransitCurrency.USD(mFare);
    }

    @Override
    public Station getStartStation() {
        return ClipperData.getStation(mAgency, mFrom, false);
    }

    @Override
    public Station getEndStation() {
        return ClipperData.getStation(mAgency, mTo, true);
    }

    @Override
    public Mode getMode() {
        switch (mTransportCode) {
            case 0x62:
                if (mAgency == ClipperData.AGENCY_BAY_FERRY
                        || mAgency == ClipperData.AGENCY_GG_FERRY)
                    return Mode.FERRY;
                if (mAgency == ClipperData.AGENCY_CALTRAIN)
                    return Trip.Mode.TRAIN;
                return Trip.Mode.TRAM;
            case 0x6f:
                return Trip.Mode.METRO;
            case 0x61:
            case 0x75:
                return Trip.Mode.BUS;
        }
        return Mode.OTHER;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mTimestamp);
        parcel.writeLong(mExitTimestamp);
        parcel.writeInt(mFare);
        parcel.writeInt(mAgency);
        parcel.writeInt(mFrom);
        parcel.writeInt(mTo);
        parcel.writeInt(mRoute);
        parcel.writeInt(mVehicleNum);
        parcel.writeInt(mTransportCode);
    }

    public int describeContents() {
        return 0;
    }
}
