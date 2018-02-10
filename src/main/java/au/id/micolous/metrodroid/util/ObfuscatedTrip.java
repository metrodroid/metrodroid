package au.id.micolous.metrodroid.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;

/**
 * Special wrapper for Trip that handles obfuscation of Trip data.
 */
class ObfuscatedTrip extends Trip implements Parcelable {
    private Calendar mStartTimestamp;
    private Calendar mEndTimestamp;

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
        long startTimestamp = parcel.readLong();
        if (startTimestamp != 0) {
            mStartTimestamp = new GregorianCalendar();
            mStartTimestamp.setTimeInMillis(startTimestamp);
        } else {
            mStartTimestamp = null;
        }

        long endTimestamp = parcel.readLong();
        if (endTimestamp != 0) {
            mEndTimestamp = new GregorianCalendar();
            mEndTimestamp.setTimeInMillis(endTimestamp);
        } else {
            mEndTimestamp = null;
        }

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
        parcel.writeLong(mStartTimestamp != null ? mStartTimestamp.getTimeInMillis() : 0);
        parcel.writeLong(mEndTimestamp != null ? mEndTimestamp.getTimeInMillis() : 0);
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
        if (realTrip.getStartTimestamp() != null) {
            mStartTimestamp = new GregorianCalendar();
            mStartTimestamp.setTimeInMillis(realTrip.getStartTimestamp().getTimeInMillis() + timeDelta);
        }

        if (realTrip.getEndTimestamp() != null) {
            mEndTimestamp = new GregorianCalendar();
            mEndTimestamp.setTimeInMillis(realTrip.getEndTimestamp().getTimeInMillis() + timeDelta);
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
    public Calendar getStartTimestamp() {
        return mStartTimestamp;
    }

    @Override
    public Calendar getEndTimestamp() {
        return mEndTimestamp;
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