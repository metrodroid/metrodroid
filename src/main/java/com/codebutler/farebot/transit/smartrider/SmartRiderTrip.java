package com.codebutler.farebot.transit.smartrider;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

/**
 * Trip on SmartRider / MyWay
 */

public class SmartRiderTrip extends Trip implements Comparable<SmartRiderTrip> {
    public static final Creator<SmartRiderTrip> CREATOR = new Creator<SmartRiderTrip>() {

        public SmartRiderTrip createFromParcel(Parcel in) {
            return new SmartRiderTrip(in);
        }

        public SmartRiderTrip[] newArray(int size) {
            return new SmartRiderTrip[size];
        }
    };

    protected SmartRiderTransitData.CardType mCardType;
    protected long mStartTime;
    protected long mEndTime;
    protected String mRouteNumber;
    protected int mCost;

    public SmartRiderTrip(Parcel parcel) {
        mCardType = SmartRiderTransitData.CardType.valueOf(parcel.readString());
        mStartTime = parcel.readLong();
        mEndTime = parcel.readLong();
        mCost = parcel.readInt();
        mRouteNumber = parcel.readString();
    }

    public SmartRiderTrip(SmartRiderTransitData.CardType cardType) {
        mCardType = cardType;
    }

    @Override
    public int compareTo(@NonNull SmartRiderTrip other) {
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
        switch (mCardType) {
            case MYWAY:
                return "ACTION";

            case SMARTRIDER:
                return "TransPerth";

            default:
                return "";
        }
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

    @Nullable
    @Override
    public Integer getFare() {
        return mCost;
    }

    @Override
    public Mode getMode() {
        switch (mCardType) {
            case MYWAY:
                return Mode.BUS;

            case SMARTRIDER:
                if ("RAIL".equalsIgnoreCase(mRouteNumber)) {
                    return Mode.TRAIN;
                } else if ("300".equals(mRouteNumber)) {
                    // TODO: verify this
                    // There is also a bus with route number 300, but it is a free service.
                    return Mode.FERRY;
                } else {
                    return Mode.BUS;
                }

            default:
                return Mode.OTHER;
        }
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
        parcel.writeString(mCardType.toString());
        parcel.writeLong(mStartTime);
        parcel.writeLong(mEndTime);
        parcel.writeInt(mCost);
        parcel.writeString(mRouteNumber);
    }

}
