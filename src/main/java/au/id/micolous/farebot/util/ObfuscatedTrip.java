package au.id.micolous.farebot.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import au.id.micolous.farebot.transit.Station;
import au.id.micolous.farebot.transit.Trip;

/**
 * Special wrapper for Trip that handles obfuscation of Trip data.
 */
class ObfuscatedTrip extends Trip implements Parcelable {
    private long mTimestamp = 0;
    private long mExitTimestamp = 0;

    private String mRouteName;
    private String mAgencyName;
    private String mShortAgencyName;
    private String mStartStationName;
    private Station mStartStation;
    private String mEndStationName;
    private Station mEndStation;
    private boolean mHasFare;
    private boolean mHasTime;
    private Mode mMode;
    private Integer mFare;

    public static final Creator<ObfuscatedTrip> CREATOR = new Creator<ObfuscatedTrip>() {
        public ObfuscatedTrip createFromParcel(Parcel parcel) {
            return new ObfuscatedTrip(parcel);
        }

        public ObfuscatedTrip[] newArray(int size) {
            return new ObfuscatedTrip[size];
        }
    };

    private ObfuscatedTrip(Parcel parcel) {
        mTimestamp = parcel.readLong();
        mExitTimestamp = parcel.readLong();
        mRouteName = parcel.readString();
        mAgencyName = parcel.readString();
        mShortAgencyName = parcel.readString();
        mStartStationName = parcel.readString();
        mEndStationName = parcel.readString();
        mHasFare = parcel.readInt() == 1;
        mHasTime = parcel.readInt() == 1;
        mMode = Mode.valueOf(parcel.readString());
        if (parcel.readInt() == 1) {
            mFare = parcel.readInt();
        }
        if (parcel.readInt() == 1) {
            mStartStation = Station.CREATOR.createFromParcel(parcel);
        }
        if (parcel.readInt() == 1) {
            mEndStation = Station.CREATOR.createFromParcel(parcel);
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mTimestamp);
        parcel.writeLong(mExitTimestamp);
        parcel.writeString(mRouteName);
        parcel.writeString(mAgencyName);
        parcel.writeString(mShortAgencyName);
        parcel.writeString(mStartStationName);
        parcel.writeString(mEndStationName);
        parcel.writeInt(mHasFare ? 1 : 0);
        parcel.writeInt(mHasTime ? 1 : 0);
        parcel.writeString(mMode.toString());
        parcel.writeInt(mFare == null ? 0 : 1);
        if (mFare != null) {
            parcel.writeInt(mFare);
        }
        parcel.writeInt(mStartStation == null ? 0 : 1);
        mStartStation.writeToParcel(parcel, i);
        parcel.writeInt(mEndStation == null ? 0 : 1);
        mEndStation.writeToParcel(parcel, i);
    }

    ObfuscatedTrip(Trip realTrip, long timeDelta, int fareOffset, double fareMultiplier) {
        if (realTrip.getTimestamp() != 0) {
            mTimestamp = realTrip.getTimestamp() + timeDelta;
        }

        if (realTrip.getExitTimestamp() != 0) {
            mExitTimestamp = realTrip.getExitTimestamp() + timeDelta;
        }

        mRouteName = realTrip.getRouteName();
        mAgencyName = realTrip.getAgencyName();
        mShortAgencyName = realTrip.getShortAgencyName();

        mStartStationName = realTrip.getStartStationName();
        mStartStation = realTrip.getStartStation();
        mEndStationName = realTrip.getEndStationName();
        mEndStation = realTrip.getEndStation();

        mHasFare = realTrip.hasFare();
        mHasTime = realTrip.hasTime();
        mMode = realTrip.getMode();

        Integer fare = realTrip.getFare();
        if (fare != null) {
            mFare = (int) ((fare + fareOffset) * fareMultiplier);

            // Match the sign of the original fare
            if ((fare >= 0 && mFare < 0) || (fare < 0 && mFare > 0)) {
                mFare *= -1;
            }
        }
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public long getExitTimestamp() {
        return mExitTimestamp;
    }

    @Override
    public String getRouteName() {
        return mRouteName;
    }

    @Override
    public String getAgencyName() {
        return mAgencyName;
    }

    @Override
    public String getShortAgencyName() {
        return mShortAgencyName;
    }

    @Override
    public String getStartStationName() {
        return mStartStationName;
    }

    @Override
    public Station getStartStation() {
        return mStartStation;
    }

    @Override
    public String getEndStationName() {
        return mEndStationName;
    }

    @Override
    public Station getEndStation() {
        return mEndStation;
    }

    @Override
    public boolean hasFare() {
        return mHasFare;
    }

    @Override
    @Nullable
    public Integer getFare() {
        return mFare;
    }

    @Override
    public Mode getMode() {
        return mMode;
    }

    @Override
    public boolean hasTime() {
        return mHasTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}