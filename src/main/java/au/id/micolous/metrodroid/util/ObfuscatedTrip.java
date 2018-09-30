/*
 * NumericalStringComparator.java
 *
 * Copyright 2017 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
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
    private boolean mHasTime;
    private Mode mMode;
    private TransitCurrency mFare;
    private String mVehicleID;
    private Integer mPassengerCount;

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
        mHasTime = parcel.readInt() == 1;
        mMode = Mode.valueOf(parcel.readString());
        if (parcel.readInt() == 1) {
            mFare = new TransitCurrency(parcel);
        }
        if (parcel.readInt() == 1) {
            mStartStation = Station.CREATOR.createFromParcel(parcel);
        }
        if (parcel.readInt() == 1) {
            mEndStation = Station.CREATOR.createFromParcel(parcel);
        }
	if (parcel.readInt() == 1) {
            mVehicleID = parcel.readString();
        }
	if (parcel.readInt() == 1) {
            mPassengerCount = parcel.readInt();
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
        parcel.writeInt(mHasTime ? 1 : 0);
        parcel.writeString(mMode.toString());
        parcel.writeInt(mFare == null ? 0 : 1);
        if (mFare != null) {
            mFare.writeToParcel(parcel, i);
        }
        parcel.writeInt(mStartStation == null ? 0 : 1);
        if (mStartStation != null) {
            mStartStation.writeToParcel(parcel, i);
        }
        parcel.writeInt(mEndStation == null ? 0 : 1);
        if (mEndStation != null) {
            mEndStation.writeToParcel(parcel, i);
        }
	    parcel.writeInt(mVehicleID == null ? 0 : 1);
        if (mVehicleID != null) {
            parcel.writeString(mVehicleID);
        }
	    parcel.writeInt(mPassengerCount == null ? 0 : 1);
        if (mPassengerCount != null) {
            parcel.writeInt(mPassengerCount);
        }
    }

    ObfuscatedTrip(Trip realTrip, long timeDelta, boolean obfuscateFares) {
        if (realTrip.getStartTimestamp() != null) {
            mStartTimestamp = new GregorianCalendar();
            mStartTimestamp.setTimeInMillis(realTrip.getStartTimestamp().getTimeInMillis() + timeDelta);
        }

        if (realTrip.getEndTimestamp() != null) {
            mEndTimestamp = new GregorianCalendar();
            mEndTimestamp.setTimeInMillis(realTrip.getEndTimestamp().getTimeInMillis() + timeDelta);
        }

        mRouteName = realTrip.getRouteName();
        mAgencyName = realTrip.getAgencyName(false);
        mShortAgencyName = realTrip.getAgencyName(true);

        mStartStation = realTrip.getStartStation();
        mEndStation = realTrip.getEndStation();

        mHasTime = realTrip.hasTime();
        mMode = realTrip.getMode();

        TransitCurrency fare = realTrip.getFare();
        if (fare != null) {
            if (obfuscateFares) {
                mFare = fare.obfuscate();
            } else {
                mFare = fare;
            }
        }

	mPassengerCount = realTrip.getPassengerCount();
	mVehicleID = realTrip.getVehicleID();
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
    public String getAgencyName(boolean isShort) {
        return isShort ? mShortAgencyName : mAgencyName;
    }

    @Override
    public Station getStartStation() {
        return mStartStation;
    }

    @Override
    public Station getEndStation() {
        return mEndStation;
    }

    @Override
    @Nullable
    public TransitCurrency getFare() {
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
