/*
 * NewShenzhenTrip.java
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

package au.id.micolous.metrodroid.transit.newshenzhen;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

public class NewShenzhenTrip extends Trip {
    public static final Creator<NewShenzhenTrip> CREATOR = new Creator<NewShenzhenTrip>() {

        public NewShenzhenTrip createFromParcel(Parcel in) {
            return new NewShenzhenTrip(in);
        }

        public NewShenzhenTrip[] newArray(int size) {
            return new NewShenzhenTrip[size];
        }
    };
    private static final int SZT_BUS = 3;
    private static final int SZT_METRO = 6;

    private final long mTime;
    private final int mCost;
    private final int mType;
    private final long mStation;
    private final static String SHENZHEN_STR = "shenzhen";


    public NewShenzhenTrip(Parcel parcel) {
        mTime = parcel.readLong();
        mCost = parcel.readInt();
        mType = parcel.readInt();
        mStation = parcel.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mTime);
        parcel.writeInt(mCost);
        parcel.writeInt(mType);
        parcel.writeLong(mStation);
    }

    public NewShenzhenTrip(int cost, long time, int agency, long station) {
        mCost = cost;
        mTime = time;
        mType = agency;
        mStation = station;
    }

    @Override
    public Station getEndStation() {
        int transport = getTransport();
        switch (transport) {
            case SZT_METRO:
                return StationTableReader.getStation(SHENZHEN_STR, (int) (mStation & ~0xff),
                        Long.toHexString(mStation >> 8)).addAttribute(
                        Utils.localizeString(R.string.szt_station_gate,
                                Integer.toHexString((int)(mStation & 0xff))));
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.CNY(mCost);
    }

    private int getTransport() {
        return (int)(mStation >> 28);
    }

    @Override
    public Mode getMode() {
        int transport = getTransport();
        switch (transport) {
            case SZT_METRO:
                return Mode.METRO;
            case SZT_BUS:
                return Mode.BUS;
            default:
                return Mode.OTHER;
        }
    }

    @Override
    public String getRouteName() {
        int transport = getTransport();
        switch (transport) {
            case SZT_BUS:
                return StationTableReader.getLineName(SHENZHEN_STR, (int) mStation);
            default:
                return null;
        }
    }

    @Override
    public String getAgencyName(boolean isShort) {
        int transport = getTransport();
        switch (transport) {
            case SZT_METRO:
                return Utils.localizeString(R.string.szt_metro);
            case SZT_BUS:
                return Utils.localizeString(R.string.szt_bus);
            default:
                return Utils.localizeString(R.string.unknown_format, transport);
        }
    }

    @Override
    public Calendar getStartTimestamp() {
        int transport = getTransport();
        if (transport == SZT_METRO)
            return null;
        return NewShenzhenTransitData.parseHexDateTime (mTime);
    }

    @Override
    public Calendar getEndTimestamp() {
        int transport = getTransport();
        if (transport != SZT_METRO)
            return null;
        return NewShenzhenTransitData.parseHexDateTime (mTime);
    }

    @Nullable
    public static NewShenzhenTrip parseTrip(byte[] data) {
        int cost;
        long time;
        int type;
        Long station;
        // 2 bytes counter
        // 3 bytes zero
        // 4 bytes cost
        cost = Utils.byteArrayToInt(data, 5,4);
        type = data[9];
        // 2 bytes zero
        station = Utils.byteArrayToLong(data, 12, 4);
        time = Utils.byteArrayToLong(data, 16, 7);
        if (cost == 0 && time == 0)
            return null;
        return new NewShenzhenTrip(cost, time, type, station);
    }
}
