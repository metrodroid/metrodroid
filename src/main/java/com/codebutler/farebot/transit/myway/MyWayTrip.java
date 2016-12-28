package com.codebutler.farebot.transit.myway;

import android.os.Parcel;
import android.support.annotation.NonNull;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.util.Utils;

import java.text.NumberFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import au.id.micolous.farebot.R;

/**
 * Trip on MyWay
 */

public class MyWayTrip extends Trip implements Comparable<MyWayTrip> {
    protected long mStartTime;
    protected long mEndTime;
    protected String mRouteNumber;
    protected int mCost;

    @Override
    public int compareTo(@NonNull MyWayTrip other) {
        return (new Long(this.mStartTime)).compareTo(other.mStartTime);
    }

    @Override
    public long getTimestamp() {
        return mStartTime;
    }

    @Override
    public long getExitTimestamp() {
        return mEndTime;
    }

    @Override
    public String getRouteName() {
        return mRouteNumber;
    }


    @Override
    public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override
    public String getAgencyName() {
        return "ACTION";
    }

    @Override
    public String getFareString() {
        if (mCost == 0) {
            return Utils.localizeString(R.string.pass_or_transfer);
        }
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)mCost / 100.);
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
    public Mode getMode() {
        // TODO: Fix this when/if Canberra gets trams.
        return Mode.BUS;
    }

    @Override
    public boolean hasTime() {
        return mStartTime != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mStartTime);
        parcel.writeLong(mEndTime);
        parcel.writeInt(mCost);
        parcel.writeString(mRouteNumber);
    }

    public MyWayTrip(Parcel parcel) {
        mStartTime = parcel.readLong();
        mEndTime = parcel.readLong();
        mCost = parcel.readInt();
        mRouteNumber = parcel.readString();
    }


    public MyWayTrip() {}

    public static final Creator<MyWayTrip> CREATOR = new Creator<MyWayTrip>() {

        public MyWayTrip createFromParcel(Parcel in) {
            return new MyWayTrip(in);
        }

        public MyWayTrip[] newArray(int size) {
            return new MyWayTrip[size];
        }
    };

}
