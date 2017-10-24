package com.codebutler.farebot.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.seq_go.SeqGoTransitData;
import com.codebutler.farebot.transit.seq_go.SeqGoTrip;

/**
 * Special wrapper for Trip that handles obfuscation of Trip data.
 */
class ObfuscatedTrip extends Trip implements Parcelable {
    private Trip mRealTrip;
    private long mTimeDelta;
    private int mFareOffset;
    private double mFareMultiplier;

    private static final TripClassLoader mTripClassLoader = new TripClassLoader();

    private static class TripClassLoader extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals(SeqGoTrip.class.getName())) {
                return SeqGoTrip.class;
            }

            throw new ClassNotFoundException(name);
        }
    }

    public static final Creator<ObfuscatedTrip> CREATOR = new Creator<ObfuscatedTrip>() {
        public ObfuscatedTrip createFromParcel(Parcel parcel) {
            return new ObfuscatedTrip(parcel);
        }

        public ObfuscatedTrip[] newArray(int size) {
            return new ObfuscatedTrip[size];
        }
    };

    private ObfuscatedTrip(Parcel parcel) {
        mRealTrip = parcel.readParcelable(mTripClassLoader);
        mTimeDelta = parcel.readLong();
        mFareOffset = parcel.readInt();
        mFareMultiplier = parcel.readDouble();
    }

    ObfuscatedTrip(Trip realTrip, long timeDelta, int fareOffset, double fareMultiplier) {
        mRealTrip = realTrip;
        mTimeDelta = timeDelta;

        mFareOffset = fareOffset;
        mFareMultiplier = fareMultiplier;
    }

    @Override
    public long getTimestamp() {
        if (mRealTrip.getTimestamp() == 0) {
            return 0;
        }

        return mRealTrip.getTimestamp() + mTimeDelta;
    }

    @Override
    public long getExitTimestamp() {
        if (mRealTrip.getExitTimestamp() == 0) {
            return 0;
        }

        return mRealTrip.getExitTimestamp() + mTimeDelta;
    }

    @Override
    public String getRouteName() {
        return mRealTrip.getRouteName();
    }

    @Override
    public String getAgencyName() {
        return mRealTrip.getAgencyName();
    }

    @Override
    public String getShortAgencyName() {
        return mRealTrip.getShortAgencyName();
    }

    @Override
    public String getStartStationName() {
        return mRealTrip.getStartStationName();
    }

    @Override
    public Station getStartStation() {
        return mRealTrip.getStartStation();
    }

    @Override
    public String getEndStationName() {
        return mRealTrip.getEndStationName();
    }

    @Override
    public Station getEndStation() {
        return mRealTrip.getEndStation();
    }

    @Override
    public boolean hasFare() {
        return mRealTrip.hasFare();
    }

    @Override
    @Nullable
    public Integer getFare() {
        Integer fare = mRealTrip.getFare();
        if (fare == null) {
            return fare;
        }
        int newfare = (int)((fare + mFareOffset) * mFareMultiplier);

        // Match the sign of the original fare
        if ((fare >= 0 && newfare < 0) || (fare < 0 && newfare > 0)) {
            newfare *= -1;
        }

        return newfare;
    }

    @Override
    public Mode getMode() {
        return mRealTrip.getMode();
    }

    @Override
    public boolean hasTime() {
        return mRealTrip.hasTime();
    }

    @Override
    public int describeContents() {
        return mRealTrip.describeContents();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(mRealTrip, i);
        parcel.writeLong(mTimeDelta);
        parcel.writeInt(mFareOffset);
        parcel.writeDouble(mFareMultiplier);
    }
}