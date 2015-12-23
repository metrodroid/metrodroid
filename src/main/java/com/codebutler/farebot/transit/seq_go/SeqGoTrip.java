package com.codebutler.farebot.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import java.util.GregorianCalendar;

/**
 * Created by michael on 23/12/15.
 */
public class SeqGoTrip extends Trip {
    int mJourneyId;
    Mode mMode;
    GregorianCalendar mStartTime;
    GregorianCalendar mEndTime;
    int mStartStation;
    int mEndStation;


    @Override
    public long getTimestamp() {
        if (mStartTime != null) {
            return mStartTime.getTimeInMillis() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public long getExitTimestamp() {
        if (mEndTime != null) {
            return mEndTime.getTimeInMillis() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getAgencyName() {
        return null;
    }

    @Override
    public String getShortAgencyName() {
        return null;
    }

    @Override
    public String getFareString() {
        return null;
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName() {
        if (mStartStation == 0) {
            return null;
        } else {
            Station s = getStartStation();
            if (s == null) {
                return Integer.toString(mStartStation);
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public Station getStartStation() {
        return SeqGoUtil.getStation(mStartStation);
    }

    @Override
    public String getEndStationName() {
        if (mEndStation == 0) {
            return null;
        } else {
            Station s = getEndStation();
            if (s == null) {
                return Integer.toString(mEndStation);
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public Station getEndStation() {
        return SeqGoUtil.getStation(mEndStation);
    }

    @Override
    public double getFare() {
        // We can't calculate fares yet.
        return 0;
    }

    @Override
    public Mode getMode() {
        return mMode;
    }

    @Override
    public boolean hasTime() {
        return mStartTime != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mJourneyId);
        parcel.writeLong(mStartTime == null ? 0 : mStartTime.getTimeInMillis());
        parcel.writeLong(mEndTime == null ? 0 : mEndTime.getTimeInMillis());
        parcel.writeString(mMode.toString());
        parcel.writeInt(mStartStation);
        parcel.writeInt(mEndStation);
    }

    public SeqGoTrip(Parcel parcel) {
        mJourneyId = parcel.readInt();
        long startTime = parcel.readLong();
        if (startTime != 0) {
            mStartTime = new GregorianCalendar();
            mStartTime.setTimeInMillis(startTime);
        }

        long endTime = parcel.readLong();
        if (endTime != 0) {
            mEndTime = new GregorianCalendar();
            mEndTime.setTimeInMillis(endTime);
        }

        mMode = Mode.valueOf(parcel.readString());
        mStartStation = parcel.readInt();
        mEndStation = parcel.readInt();
    }

    public SeqGoTrip() {}

    public static final Parcelable.Creator<SeqGoTrip> CREATOR = new Parcelable.Creator<SeqGoTrip>() {

        public SeqGoTrip createFromParcel(Parcel in) {
            return new SeqGoTrip(in);
        }

        public SeqGoTrip[] newArray(int size) {
            return new SeqGoTrip[size];
        }
    };
}
