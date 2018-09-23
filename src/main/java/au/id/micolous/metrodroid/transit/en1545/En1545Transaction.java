/*
 * En1545Transaction.java
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

package au.id.micolous.metrodroid.transit.en1545;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public abstract class En1545Transaction implements Parcelable {
    protected final En1545Parsed mParsed;

    private static final int EVENT_TYPE_BOARD = 1;
    private static final int EVENT_TYPE_EXIT = 2;
    private static final int EVENT_TYPE_BOARD_TRANSFER = 6;
    private static final int EVENT_TYPE_EXIT_TRANSFER = 7;
    public static final int EVENT_TYPE_TOPUP = 13;
    public static final int EVENT_TYPE_CANCELLED = 9;

    private static final int TRANSPORT_BUS = 1;
    private static final int TRANSPORT_INTERCITY_BUS = 2;
    public static final int TRANSPORT_METRO = 3;
    public static final int TRANSPORT_TRAM = 4;
    public static final int TRANSPORT_TRAIN = 5;
    private static final int TRANSPORT_FERRY = 6;
    private static final int TRANSPORT_PARKING = 8;
    private static final int TRANSPORT_TAXI = 9;
    private static final int TRANSPORT_TOPUP = 11;

    public En1545Transaction(byte[] data, En1545Field fields) {
        mParsed = En1545Parser.parse(data, fields);
    }

    public En1545Transaction(Parcel parcel) {
        mParsed = new En1545Parsed(parcel);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mParsed.writeToParcel(dest, flags);
    }

    protected String getRouteNameReal() {
        String route = getLookup().getRouteName(
                mParsed.getInt("EventRouteNumber"),
                mParsed.getInt("EventRouteVariant"),
                getAgency(), getTransport());
        if (route != null)
            return route;
        Station st = getStation();
        if (st == null)
            return null;
        return st.getLineName();
    }

    public String getRouteName() {
        String route = getRouteNameReal();
        ArrayList <String> components = new ArrayList<>();
        if (route != null)
            components.add(route);
        int pax = mParsed.getIntOrZero("EventPassengerCount");
        if (pax != 0)
            components.add(Utils.localizePlural(R.plurals.passenger_count, pax, pax));
        int vhn = mParsed.getIntOrZero("EventVehiculeId");
        if (vhn != 0)
            components.add(Utils.localizeString(R.string.vehicle_number, Integer.toString(vhn)));
        if (components.isEmpty())
            return null;
        return TextUtils.join(", ", components);
    }

    private int getEventCode() {
        return mParsed.getIntOrZero("EventCode");
    }

    private static int getTransport(int eventCode) {
        return eventCode >> 4;
    }

    protected int getTransport() {
        return getTransport(getEventCode());
    }

    protected Integer getAgency() {
        return mParsed.getInt("EventServiceProvider");
    }

    public String getAgencyName(boolean isShort) {
        return getLookup().getAgencyName(getAgency(), isShort);
    }

    @Nullable
    public Station getStation(Integer station) {
        if (station == null)
            return null;
        return getLookup().getStation(station, getAgency(), getTransport());
    }

    @Nullable
    public Station getStation() {
        return getStation(getStationId());
    }

    public Calendar getTimestamp() {
        return mParsed.getTimeStamp("Event", getLookup().getTimeZone());
    }

    private static Trip.Mode eventCodeToMode(int ec) {
        if ((ec & 0xf) == EVENT_TYPE_TOPUP)
            return Trip.Mode.TICKET_MACHINE;
        switch (getTransport(ec)) {
            case TRANSPORT_BUS:
            case TRANSPORT_INTERCITY_BUS:
                return Trip.Mode.BUS;
            case TRANSPORT_METRO:
                return Trip.Mode.METRO;
            case TRANSPORT_TRAM:
                return Trip.Mode.TRAM;
            case TRANSPORT_TRAIN:
                return Trip.Mode.TRAIN;
            case TRANSPORT_FERRY:
                return Trip.Mode.FERRY;
            case TRANSPORT_PARKING:
            case TRANSPORT_TAXI:
            default:
                return Trip.Mode.OTHER;
            case TRANSPORT_TOPUP:
                return Trip.Mode.TICKET_MACHINE;
        }
    }

    public Trip.Mode getMode() {
        Integer ec = mParsed.getInt("EventCode");
        if (ec != null)
            return eventCodeToMode(ec);
        return getLookup().getMode(getAgency(), mParsed.getInt("EventRouteNumber"));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected abstract En1545Lookup getLookup();

    @Nullable
    public TransitCurrency getFare() {
        Integer x = mParsed.getInt("EventPriceAmount");
        if (x == null)
            return null;
        return getLookup().parseCurrency(x);
    }

    protected int getEventType() {
        return (getEventCode() & 0xf);
    }

    protected boolean isTapOn() {
        int eventCode = getEventCode();
        return (eventCode & 0xf) == EVENT_TYPE_BOARD || (eventCode & 0xf) == EVENT_TYPE_BOARD_TRANSFER;
    }

    protected boolean isTapOff() {
        int eventCode = getEventCode();
        return (eventCode & 0xf) == EVENT_TYPE_EXIT || (eventCode & 0xf) == EVENT_TYPE_EXIT_TRANSFER;
    }

    protected boolean isSameTrip(En1545Transaction other) {
        return getTransport() == (other.getTransport())
                && mParsed.getIntOrZero("EventServiceProvider") == other.mParsed.getIntOrZero("EventServiceProvider")
                && mParsed.getIntOrZero("EventRouteNumber") == other.mParsed.getIntOrZero("EventRouteNumber")
                && mParsed.getIntOrZero("EventRouteVariant") == other.mParsed.getIntOrZero("EventRouteVariant");
    }

    boolean shouldBeMerged(En1545Transaction other) {
        return isTapOn() && other.isTapOff() && isSameTrip(other);
    }

    private Integer getStationId() {
        return mParsed.getInt("EventLocationId");
    }
}
