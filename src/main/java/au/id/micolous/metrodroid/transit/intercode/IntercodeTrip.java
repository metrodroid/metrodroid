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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedString;
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed;
import au.id.micolous.metrodroid.transit.en1545.En1545Parser;

class IntercodeTrip extends Trip {
    private static final int TRANSPORT_BUS = 1;
    private static final int TRANSPORT_INTERCITY_BUS = 2;
    public static final int TRANSPORT_METRO = 3;
    public static final int TRANSPORT_TRAM = 4;
    public static final int TRANSPORT_TRAIN = 5;
    private static final int TRANSPORT_FERRY = 6;
    private static final int TRANSPORT_PARKING = 8;
    private static final int TRANSPORT_TAXI = 9;
    private static final int TRANSPORT_TOPUP = 11;
    private final int mAgency;
    private final int mStationId;
    private final int mNetworkId;
    private final int mEventCode;

    public static final Creator<IntercodeTrip> CREATOR = new Creator<IntercodeTrip>() {
        @NonNull
        public IntercodeTrip createFromParcel(Parcel parcel) {
            return new IntercodeTrip(parcel);
        }

        @NonNull
        public IntercodeTrip[] newArray(int size) {
            return new IntercodeTrip[size];
        }
    };
    private final En1545Parsed mParsed;

    private static final En1545Field tripFields = new En1545Container(
            new En1545FixedInteger("EventDate", 14),
            new En1545FixedInteger("EventTime", 11),
            new En1545Bitmap(
                    new En1545FixedInteger("EventDisplayData", 8),
                    new En1545FixedInteger("EventNetworkId", 24),
                    new En1545FixedInteger("EventCode", 8),
                    new En1545FixedInteger("EventResult", 8),
                    new En1545FixedInteger("EventServiceProvider", 8),
                    new En1545FixedInteger("EventNotOkCounter", 8),
                    new En1545FixedInteger("EventSerialNumber", 24),
                    new En1545FixedInteger("EventDestination", 16),
                    new En1545FixedInteger("EventLocationId", 16),
                    new En1545FixedInteger("EventLocationGate", 8),
                    new En1545FixedInteger("EventDevice", 16),
                    new En1545FixedInteger("EventRouteNumber", 16),
                    new En1545FixedInteger("EventRouteVariant", 8),
                    new En1545FixedInteger("EventJourneyRun", 16),
                    new En1545FixedInteger("EventVehiculeId", 16),
                    new En1545FixedInteger("EventVehiculeClass", 8),
                    new En1545FixedInteger("EventLocationType", 5),
                    new En1545FixedString("EventEmployee", 240),
                    new En1545FixedInteger("EventLocationReference", 16),
                    new En1545FixedInteger("EventJourneyInterchanges", 8),
                    new En1545FixedInteger("EventPeriodJourneys", 16),
                    new En1545FixedInteger("EventTotalJourneys", 16),
                    new En1545FixedInteger("EventJourneyDistance", 16),
                    new En1545FixedInteger("EventPriceAmount", 16),
                    new En1545FixedInteger("EventPriceUnit", 16),
                    new En1545FixedInteger("EventContractPointer", 5),
                    new En1545FixedInteger("EventAuthenticator", 16),
                    new En1545FixedInteger("EventBitmapExtra", 5)
            )
    );
    private int mEndLocationId;
    private int mEndDate;
    private int mEndTime;

    public IntercodeTrip(byte[] data, int networkId) {
        mParsed = En1545Parser.parse(data, tripFields);

        mStationId = mParsed.getIntOrZero("EventLocationId");
        mAgency = mParsed.getIntOrZero("EventServiceProvider");
        mEventCode = mParsed.getIntOrZero("EventCode");
        Integer nid = mParsed.getInt("EventNetworkId");
        if (nid != null)
            mNetworkId = nid;
        else
            mNetworkId = networkId;
    }

    @Override
    public String getRouteName() {
        return IntercodeTransitData.getLookup(mNetworkId).getRouteName(
            mParsed.getInt("EventRouteNumber"),
                mParsed.getInt("EventRouteVariant"),
        mAgency, mEventCode >> 4);
    }

    @Override
    public String getAgencyName() {
        return IntercodeTransitData.getAgencyName(mNetworkId, mAgency, true);
    }

    @Override
    public String getShortAgencyName() {
        return IntercodeTransitData.getAgencyName(mNetworkId, mAgency, true);
    }

    @Nullable
    public Station getStation(int station) {
        return IntercodeTransitData.getLookup(mNetworkId).getStation(station, mAgency,
                mEventCode >> 4);
    }

    @Nullable
    @Override
    public Station getStartStation() {
        return getStation(mStationId);
    }

    @Nullable
    @Override
    public Station getEndStation() {
        return getStation(mEndLocationId);
    }

    @Override
    public Calendar getStartTimestamp() {
        return IntercodeTransitData.parseTime(
                mParsed.getIntOrZero("EventDate"),
                mParsed.getIntOrZero("EventTime"));
    }

    @Override
    public Calendar getEndTimestamp() {
        return IntercodeTransitData.parseTime(mEndDate, mEndTime);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        Integer x = mParsed.getInt("EventPriceAmount");
        if (x == null)
            return null;
        return TransitCurrency.EUR(x);
    }

    @Override
    public Mode getMode() {
        switch (mEventCode >> 4) {
            case TRANSPORT_BUS:
            case TRANSPORT_INTERCITY_BUS:
                return Mode.BUS;
            case TRANSPORT_METRO:
                return Mode.METRO;
            case TRANSPORT_TRAM:
                return Mode.TRAM;
            case TRANSPORT_TRAIN:
                return Mode.TRAIN;
            case TRANSPORT_FERRY:
                return Mode.FERRY;
            case TRANSPORT_PARKING:
            case TRANSPORT_TAXI:
            default:
                return Mode.OTHER;
            case TRANSPORT_TOPUP:
                return Mode.TICKET_MACHINE;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mNetworkId);
        dest.writeInt(mEndLocationId);
        dest.writeInt(mEndDate);
        dest.writeInt(mEndTime);

        mParsed.writeToParcel(dest, flags);
    }

    public IntercodeTrip(Parcel parcel) {
        mNetworkId = parcel.readInt();
        mEndLocationId = parcel.readInt();
        mEndDate = parcel.readInt();
        mEndTime = parcel.readInt();
        mParsed = new En1545Parsed(parcel);
        mStationId = mParsed.getIntOrZero("EventLocationId");
        mAgency = mParsed.getIntOrZero("EventServiceProvider");
        mEventCode = mParsed.getIntOrZero("EventCode");
    }

    private boolean isTapOn() {
        return (mEventCode & 0xf) == 1 || (mEventCode & 0xf) == 6;
    }

    private boolean isTapOff() {
        return (mEventCode & 0xf) == 2 || (mEventCode & 0xf) == 7;
    }

    private boolean isSameTrip(IntercodeTrip other) {
        return (mEventCode >> 4) == (other.mEventCode >> 4)
                && mParsed.getIntOrZero("EventServiceProvider") == other.mParsed.getIntOrZero("EventServiceProvider")
                && mParsed.getIntOrZero("EventRouteNumber") == other.mParsed.getIntOrZero("EventRouteNumber")
                && mParsed.getIntOrZero("EventRouteVariant") == other.mParsed.getIntOrZero("EventRouteVariant");
    }

    public boolean shouldBeMerged(IntercodeTrip other) {
        return isTapOn() && other.isTapOff() && isSameTrip(other);
    }

    public void merge(IntercodeTrip other) {
        mEndLocationId = other.mStationId;
        mEndDate = other.mParsed.getIntOrZero("EventDate");
        mEndTime = other.mParsed.getIntOrZero("EventTime");
    }
}
