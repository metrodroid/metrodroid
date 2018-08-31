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
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed;
import au.id.micolous.metrodroid.transit.en1545.En1545Parser;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

class RavKavTrip extends Trip {
    private static final int EGGED = 0x3;
    private static final int EVENT_TYPE_CANCELLED = 9;
    private static final int EVENT_TYPE_TAPON = 1;
    private static final int EVENT_TYPE_TAPOFF = 2;
    private static final int EVENT_TYPE_TRANSIT = 6;
    private static final int EVENT_TYPE_TOPUP = 13;
    private static final String RAVKAV_STR = "ravkav";
    private final int mModeCode;
    private final int mEventType;
    private final En1545Parsed mParsed;
    private final int mTime;
    private final int mAgency;
    private int mEndLocation;
    private int mEndTime;

    public static final Creator<RavKavTrip> CREATOR = new Creator<RavKavTrip>() {
        public RavKavTrip createFromParcel(Parcel parcel) {
            return new RavKavTrip(parcel);
        }

        public RavKavTrip[] newArray(int size) {
            return new RavKavTrip[size];
        }
    };

    static final TimeZone TZ = TimeZone.getTimeZone("Asia/Jerusalem");
    private static final long RAVKAV_EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(1997, Calendar.JANUARY, 1, 2, 0, 0);

        RAVKAV_EPOCH = epoch.getTimeInMillis();
    }

    private static final En1545Field tripFields = new En1545Container(
            new En1545Bitmap(
                    new En1545FixedInteger("Location", 16),
                    new En1545FixedInteger("LineNumber", 16),
                    new En1545FixedInteger("StopEnRoute", 8),
                    new En1545FixedInteger("Unknown1", 12),
                    new En1545FixedInteger("Vehicle", 14),
                    new En1545FixedInteger("Unknown2", 4),
                    new En1545FixedInteger("Unknown3", 8)
            ),
            new En1545Bitmap(
                    new En1545Container(
                            new En1545FixedInteger("RouteSystem", 10),
                            new En1545FixedInteger("FareCode", 8),
                            new En1545FixedInteger("Price", 16)
                    ),
                    new En1545FixedInteger("Unknown4", 32),
                    new En1545FixedInteger("Unknown5", 32)
            )
    );

    public RavKavTrip(byte[] data) {
        // 3 bits version number
        mAgency = Utils.getBitsFromBuffer(data, 3, 8);
        // 4 bits contract ID
        mModeCode = Utils.getBitsFromBuffer(data, 15, 4);
        mEventType = Utils.getBitsFromBuffer(data, 19, 4);
        mTime = Utils.getBitsFromBuffer(data, 23, 30);
        // 1 bit transit/transfer flag
        // 30 bits: first board time
        // 32 bits: contract prefs
        mParsed = En1545Parser.parse(data, 116, tripFields);
    }

    public static Calendar parseTime(int val) {
        if (val == 0)
            return null;
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(RAVKAV_EPOCH);
        g.add(Calendar.SECOND, val);
        return g;
    }

    @Override
    public String getRouteName() {
        Integer route = mParsed.getInt("LineNumber");
        if (route == null)
            return null;
        if (mAgency == EGGED)
            return Integer.toString(route%1000);
        return Integer.toString(route);
    }

    @Override
    public Calendar getStartTimestamp() {
        return parseTime(mTime);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        if (mEventType == EVENT_TYPE_TOPUP && mAgency == 0x19)
            return Utils.localizeString(R.string.ravkav_agency_topup_app);
        return StationTableReader.getOperatorName(RAVKAV_STR, mAgency, isShort);
    }

    @Nullable
    @Override
    public Station getStartStation() {
        int stationId = mParsed.getIntOrZero("Location");
        if (stationId == 0)
            return null;
        return StationTableReader.getStation(RAVKAV_STR, stationId);
    }

    @Nullable
    @Override
    public Station getEndStation() {
        if (mEndLocation == 0)
            return null;
        return StationTableReader.getStation(RAVKAV_STR, mEndLocation);
    }

    @Override
    public Calendar getEndTimestamp() {
        return parseTime(mEndTime);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        Integer price = mParsed.getInt("Price");
        if (price == null)
            return null;
        return new TransitCurrency(price, "ILS");
    }

    @Override
    public Mode getMode() {
        if (mEventType == EVENT_TYPE_TOPUP)
            return Mode.TICKET_MACHINE;

        switch (mModeCode) {
            case 1:
                return Mode.BUS;
            case 4:
                return Mode.TRAM;
            default:
                return Mode.BUS;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTime);
        dest.writeInt(mAgency);
        dest.writeInt(mModeCode);
        dest.writeInt(mEventType);
        dest.writeInt(mEndLocation);
        dest.writeInt(mEndTime);
        mParsed.writeToParcel(dest, flags);
    }

    public RavKavTrip(Parcel parcel) {
        mTime = parcel.readInt();
        mAgency = parcel.readInt();
        mModeCode = parcel.readInt();
        mEventType = parcel.readInt();
        mEndLocation = parcel.readInt();
        mEndTime = parcel.readInt();
        mParsed = new En1545Parsed(parcel);
    }

    public boolean shouldBeDropped() {
        return mEventType == EVENT_TYPE_CANCELLED;
    }

    public boolean shouldBeMerged(RavKavTrip t) {
        return mAgency == t.mAgency && mModeCode == t.mModeCode
                && (mEventType == EVENT_TYPE_TAPON || mEventType == EVENT_TYPE_TRANSIT)
                && t.mEventType == EVENT_TYPE_TAPOFF;
    }

    public void merge(RavKavTrip t) {
        mEndLocation = t.mParsed.getIntOrZero("Location");
        mEndTime = t.mTime;
    }
}
