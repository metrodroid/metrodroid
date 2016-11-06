/*
 * NextfareTrip.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package com.codebutler.farebot.transit.nextfare;

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
 * Represents trips on Nextfare
 */
public class NextfareTrip extends Trip implements Comparable<NextfareTrip> {
    protected int mJourneyId;
    protected Mode mMode;
    protected int mModeInt;
    protected GregorianCalendar mStartTime;
    protected GregorianCalendar mEndTime;
    protected int mStartStation;
    protected int mEndStation;
    protected boolean mContinuation;
    protected int mCost;

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

    public GregorianCalendar getStartTime() {
        if (mStartTime == null) {
            return null;
        }

        return (GregorianCalendar)mStartTime.clone();
    }

    public GregorianCalendar getEndTime() {
        if (mEndTime == null) {
            return null;
        }

        return (GregorianCalendar)mEndTime.clone();
    }

    @Override
    public String getRouteName() {
        return null;
    }


    @Override
    public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override
    public String getAgencyName() {
        return null;
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
        return Integer.toString(mStartStation);
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    @Override
    public String getEndStationName() {
        if (mEndTime != null) {
            return Integer.toString(mEndStation);
        } else {
            return null;
        }
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
        parcel.writeInt(mModeInt);
    }

    public NextfareTrip(Parcel parcel) {
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
        mModeInt = parcel.readInt();
    }


    public NextfareTrip() {}

    public static final Creator<NextfareTrip> CREATOR = new Creator<NextfareTrip>() {

        public NextfareTrip createFromParcel(Parcel in) {
            return new NextfareTrip(in);
        }

        public NextfareTrip[] newArray(int size) {
            return new NextfareTrip[size];
        }
    };

    @Override
    public int compareTo(@NonNull NextfareTrip other) {
        return this.mStartTime.compareTo(other.mStartTime);
    }
}
