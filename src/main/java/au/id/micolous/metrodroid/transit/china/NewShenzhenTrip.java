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

package au.id.micolous.metrodroid.transit.china;

import android.os.Parcel;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

public class NewShenzhenTrip extends ChinaTrip {
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
    private final static String SHENZHEN_STR = "shenzhen";


    private NewShenzhenTrip(Parcel parcel) {
        super(parcel);
    }

    public NewShenzhenTrip(byte[]data) {
        super(data);
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

    @Override
    public Mode getMode() {
        if (isTopup())
            return Mode.TICKET_MACHINE;
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
        return getTimestamp();
    }

    @Override
    public Calendar getEndTimestamp() {
        int transport = getTransport();
        if (transport != SZT_METRO)
            return null;
        return getTimestamp();
    }
}
