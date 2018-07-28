/*
 * RavKavTrip.java
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

package au.id.micolous.metrodroid.transit.ravkav;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

class RavKavTrip extends Trip {
    private static final int EGGED = 0x62;
    private int mTime;
    private int mAgency;
    private String mAgencyName;
    private Integer mPrice;
    private Integer mRoute;
    private Mode mMode;

    public static final Creator<RavKavTrip> CREATOR = new Creator<RavKavTrip>() {
        public RavKavTrip createFromParcel(Parcel parcel) {
            return new RavKavTrip(parcel);
        }

        public RavKavTrip[] newArray(int size) {
            return new RavKavTrip[size];
        }
    };

    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Jerusalem");
    private static final long RAVKAV_EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(1997, Calendar.JANUARY, 1, 2, 0, 0);

        RAVKAV_EPOCH = epoch.getTimeInMillis();
    }

    public RavKavTrip(byte[] data) {
        mAgency = Utils.getBitsFromBuffer(data, 0, 16);
        int modeCode = Utils.getBitsFromBuffer(data, 16, 4);
        mTime = Utils.getBitsFromBuffer(data, 23, 30);
        switch (modeCode) {
            case 2:
                mMode = Mode.BUS;
                break;
            case 3:
                mMode = Mode.TICKET_MACHINE;
                break;
            case 8:
                mMode = Mode.TRAM;
                break;
            default:
                mMode = Mode.BUS;
                break;
        }
        switch (mAgency) {
            case EGGED:
                mRoute = Utils.getBitsFromBuffer(data, 140, 9);
                mPrice = Utils.getBitsFromBuffer(data, 194, 16);
                mAgencyName = "Egged";
                break;
            case 0xa2:
                mRoute = Utils.getBitsFromBuffer(data, 140, 15);
                mPrice = Utils.getBitsFromBuffer(data, 202, 16);
                mAgencyName = "Dan";
                break;
            case 0x2a2:
                mRoute = Utils.getBitsFromBuffer(data, 140, 15);
                mPrice = Utils.getBitsFromBuffer(data, 210, 16);
                mAgencyName = "City Pass";
                break;
            case 0x322:
                mAgencyName = "RavKav Top-up app";
                break;
            default:
                mAgencyName = Integer.toHexString(mAgency);
                break;
        }
        if (modeCode == 3) {
            // It seems that topup value is not recorded.
            mRoute = null;
            mPrice = null;
        }
    }

    private static Calendar parseTime(int val) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(RAVKAV_EPOCH);
        g.add(Calendar.SECOND, val);
        return g;
    }

    @Override
    public String getRouteName() {
        return mRoute == null ? null : Integer.toString(mRoute);
    }

    @Override
    public Calendar getStartTimestamp() {
        return parseTime(mTime);
    }

    @Override
    public String getAgencyName() {
        return mAgencyName;
    }

    @Override
    public boolean hasFare() {
        return mPrice != null;
    }

    @Nullable
    @Override
    public Integer getFare() {
        return mPrice;
    }

    @Override
    public Mode getMode() {
        return mMode;
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTime);
        dest.writeInt(mAgency);
        dest.writeString(mAgencyName);
        dest.writeString(mMode.toString());
        if (mPrice != null && mRoute != null) {
            dest.writeInt(1);
            dest.writeInt(mPrice);
            dest.writeInt(mRoute);
        } else
            dest.writeInt(0);
    }

    public RavKavTrip(Parcel parcel) {
        mTime = parcel.readInt();
        mAgency = parcel.readInt();
        mAgencyName = parcel.readString();
        mMode = Mode.valueOf(parcel.readString());
        if (parcel.readInt() != 0) {
            mPrice = parcel.readInt();
            mRoute = parcel.readInt();
        }
    }

    public boolean shouldBeMerged(RavKavTrip t) {
        // Topup creates 2 entries. One of them is with
        // Egged agency independently of real agency
        if (mMode.equals(Mode.TICKET_MACHINE) && t.mMode.equals(Mode.TICKET_MACHINE)
                && mTime == t.mTime)
            return true;
        return false;
    }

    public void merge(RavKavTrip t) {
        if (mMode.equals(Mode.TICKET_MACHINE)) {
            if (mAgency == EGGED) {
                mAgencyName = t.mAgencyName;
                mAgency = t.mAgency;
            }
            return;
        }
    }
}
