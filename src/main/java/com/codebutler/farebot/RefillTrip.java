package com.codebutler.farebot;

import android.os.Parcel;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

/**
 * Wrapper around Refills to make them like Trips, so Trips become like history.  This is similar
 * to what the Japanese cards (Edy, Suica) already had implemented for themselves.
 */
public class RefillTrip extends Trip {
    protected Refill mRefill;

    public RefillTrip(Refill refill) {
        this.mRefill = refill;
    }

    @Override
    public long getTimestamp() {
        return mRefill.getTimestamp();
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getAgencyName() {
        return mRefill.getAgencyName();
    }

    @Override
    public String getShortAgencyName() {
        return mRefill.getShortAgencyName();
    }

    @Override
    public String getFareString() {
        return mRefill.getAmountString();
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName() {
        return null;
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    @Override
    public String getEndStationName() {
        return null;
    }

    @Override
    public Station getEndStation() {
        return null;
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public Trip.Mode getMode() {
        return Mode.TICKET_MACHINE;
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
        mRefill.writeToParcel(dest, flags);
    }

}
