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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import au.id.micolous.metrodroid.time.Timestamp;
import au.id.micolous.metrodroid.time.TimestampFormatterKt;
import org.jetbrains.annotations.NonNls;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public abstract class En1545Transaction extends Transaction {
    public static final String EVENT_ROUTE_NUMBER = "EventRouteNumber";
    public static final String EVENT_ROUTE_VARIANT = "EventRouteVariant";
    public static final String EVENT_PASSENGER_COUNT = "EventPassengerCount";
    public static final String EVENT_VEHICLE_ID = "EventVehicleId";
    public static final String EVENT_CODE = "EventCode";
    public static final String EVENT_SERVICE_PROVIDER = "EventServiceProvider";
    public static final String EVENT = "Event";
    public static final String EVENT_PRICE_AMOUNT = "EventPriceAmount";
    public static final String EVENT_LOCATION_ID = "EventLocationId";
    public static final String EVENT_UNKNOWN_A = "EventUnknownA";
    public static final String EVENT_UNKNOWN_B = "EventUnknownB";
    public static final String EVENT_UNKNOWN_C = "EventUnknownC";
    public static final String EVENT_UNKNOWN_D = "EventUnknownD";
    public static final String EVENT_UNKNOWN_E = "EventUnknownE";
    public static final String EVENT_UNKNOWN_F = "EventUnknownF";
    public static final String EVENT_UNKNOWN_G = "EventUnknownG";
    public static final String EVENT_UNKNOWN_H = "EventUnknownH";
    public static final String EVENT_UNKNOWN_I = "EventUnknownI";
    public static final String EVENT_CONTRACT_POINTER = "EventContractPointer";
    public static final String EVENT_SERIAL_NUMBER = "EventSerialNumber";
    public static final String EVENT_AUTHENTICATOR = "EventAuthenticator";
    public static final String EVENT_NETWORK_ID = "EventNetworkId";
    public static final String EVENT_FIRST_STAMP = "EventFirstStamp";
    public static final String EVENT_FIRST_LOCATION_ID = "EventFirstLocationId";
    public static final String EVENT_DEVICE_ID = "EventDeviceId";
    public static final String EVENT_RESULT = "EventResult";
    @NonNls
    public static final String EVENT_DISPLAY_DATA = "EventDisplayData";
    @NonNls
    public static final String EVENT_NOT_OK_COUNTER = "EventNotOkCounter";
    @NonNls
    public static final String EVENT_DESTINATION = "EventDestination";
    @NonNls
    public static final String EVENT_LOCATION_GATE = "EventLocationGate";
    @NonNls
    public static final String EVENT_DEVICE = "EventDevice";
    @NonNls
    public static final String EVENT_JOURNEY_RUN = "EventJourneyRun";
    @NonNls
    public static final String EVENT_VEHICULE_CLASS = "EventVehiculeClass";
    @NonNls
    public static final String EVENT_LOCATION_TYPE = "EventLocationType";
    @NonNls
    public static final String EVENT_EMPLOYEE = "EventEmployee";
    @NonNls
    public static final String EVENT_LOCATION_REFERENCE = "EventLocationReference";
    @NonNls
    public static final String EVENT_JOURNEY_INTERCHANGES = "EventJourneyInterchanges";
    @NonNls
    public static final String EVENT_PERIOD_JOURNEYS = "EventPeriodJourneys";
    @NonNls
    public static final String EVENT_TOTAL_JOURNEYS = "EventTotalJourneys";
    @NonNls
    public static final String EVENT_JOURNEY_DISTANCE = "EventJourneyDistance";
    @NonNls
    public static final String EVENT_PRICE_UNIT = "EventPriceUnit";
    @NonNls
    public static final String EVENT_DATA_SIMULATION = "EventDataSimulation";
    @NonNls
    public static final String EVENT_DATA_TRIP = "EventDataTrip";
    @NonNls
    public static final String EVENT_DATA_ROUTE_DIRECTION = "EventDataRouteDirection";
    protected final En1545Parsed mParsed;

    private static final int EVENT_TYPE_BOARD = 1;
    private static final int EVENT_TYPE_EXIT = 2;
    private static final int EVENT_TYPE_BOARD_TRANSFER = 6;
    private static final int EVENT_TYPE_EXIT_TRANSFER = 7;
    public static final int EVENT_TYPE_TOPUP = 13;
    public static final int EVENT_TYPE_CANCELLED = 9;

    @VisibleForTesting
    public static final int TRANSPORT_BUS = 1;
    private static final int TRANSPORT_INTERCITY_BUS = 2;
    public static final int TRANSPORT_METRO = 3;
    public static final int TRANSPORT_TRAM = 4;
    public static final int TRANSPORT_TRAIN = 5;
    private static final int TRANSPORT_FERRY = 6;
    private static final int TRANSPORT_PARKING = 8;
    private static final int TRANSPORT_TAXI = 9;
    private static final int TRANSPORT_TOPUP = 11;

    private static final String TAG = En1545Transaction.class.getSimpleName();

    public En1545Transaction(ImmutableByteArray data, En1545Field fields) {
        this(En1545Parser.parse(data, fields));
    }

    public En1545Transaction(En1545Parsed parsed) {
        mParsed = parsed;
    }

    public En1545Transaction(Parcel parcel) {
        mParsed = new En1545Parsed(parcel);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mParsed.writeToParcel(dest, flags);
    }

    @Nullable
    protected Integer getRouteNumber() {
        return mParsed.getInt(EVENT_ROUTE_NUMBER);
    }

    @Nullable
    protected Integer getRouteVariant() {
        return mParsed.getInt(EVENT_ROUTE_VARIANT);
    }

    @NonNull
    @Override
    public List<String> getRouteNames() {
        String route = getLookup().getRouteName(
                getRouteNumber(),
                getRouteVariant(),
                getAgency(), getTransport());
        if (route != null) {
            return Collections.singletonList(route);
        }

        // Get the line name from the station.
        Station st = getStation();
        if (st == null) {
            return Collections.emptyList();
        }

        return st.getLineNames();
    }

    @NonNull
    @Override
    public List<String> getHumanReadableLineIDs() {
        String route = getLookup().getHumanReadableRouteId(
                getRouteNumber(),
                getRouteVariant(),
                getAgency(), getTransport());

        if (route != null) {
            return Collections.singletonList(route);
        }

        // Get the line name from the station.
        Station st = getStation();
        if (st == null) {
            return Collections.emptyList();
        }

        return st.getHumanReadableLineIds();
    }

    public int getPassengerCount() {
        return mParsed.getIntOrZero(EVENT_PASSENGER_COUNT);
    }

    public String getVehicleID() {
        int id = mParsed.getIntOrZero(EVENT_VEHICLE_ID);
        return id == 0 ? null : Integer.toString(id);
    }

    private int getEventCode() {
        return mParsed.getIntOrZero(EVENT_CODE);
    }

    private static int getTransport(int eventCode) {
        return eventCode >> 4;
    }

    protected int getTransport() {
        return getTransport(getEventCode());
    }

    protected Integer getAgency() {
        return mParsed.getInt(EVENT_SERVICE_PROVIDER);
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

    public Timestamp getTimestamp() {
        return TimestampFormatterKt.calendar2ts(mParsed.getTimeStamp(EVENT, getLookup().getTimeZone()));
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
        Integer ec = mParsed.getInt(EVENT_CODE);
        if (ec != null)
            return eventCodeToMode(ec);
        return getLookup().getMode(getAgency(), mParsed.getInt(EVENT_ROUTE_NUMBER));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected abstract En1545Lookup getLookup();

    @Nullable
    public TransitCurrency getFare() {
        Integer x = mParsed.getInt(EVENT_PRICE_AMOUNT);
        if (x == null)
            return null;
        return getLookup().parseCurrency(x);
    }

    protected int getEventType() {
        return (getEventCode() & 0xf);
    }

    @Override
    protected boolean isTapOn() {
        int eventCode = getEventType();
        return eventCode == EVENT_TYPE_BOARD || eventCode == EVENT_TYPE_BOARD_TRANSFER;
    }

    @Override
    public boolean isTapOff() {
        int eventCode = getEventType();
        return eventCode == EVENT_TYPE_EXIT || eventCode == EVENT_TYPE_EXIT_TRANSFER;
    }

    @Override
    public boolean isTransfer() {
        int eventCode = getEventType();
        return eventCode == EVENT_TYPE_BOARD_TRANSFER || eventCode == EVENT_TYPE_EXIT_TRANSFER;
    }

    @Override
    protected boolean isSameTrip(@NonNull Transaction otherx) {
        if (!(otherx instanceof En1545Transaction))
            return false;
        En1545Transaction other = (En1545Transaction) otherx;
        return getTransport() == (other.getTransport())
                && mParsed.getIntOrZero(EVENT_SERVICE_PROVIDER) == other.mParsed.getIntOrZero(EVENT_SERVICE_PROVIDER)
                && mParsed.getIntOrZero(EVENT_ROUTE_NUMBER) == other.mParsed.getIntOrZero(EVENT_ROUTE_NUMBER)
                && mParsed.getIntOrZero(EVENT_ROUTE_VARIANT) == other.mParsed.getIntOrZero(EVENT_ROUTE_VARIANT);
    }

    protected Integer getStationId() {
        return mParsed.getInt(EVENT_LOCATION_ID);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + mParsed.toString();
    }

    public boolean isRejected() {
        // 0x2: The tap-on was rejected (insufficient funds, Adelaide Metrocard).
        // 0xb: The tap-on was rejected (outside of validity zone, RicaricaMi).
        // Successful events don't set EVENT_RESULT or set it to 0.
        return mParsed.getIntOrZero(EVENT_RESULT) != 0;
    }
}
