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
package au.id.micolous.metrodroid.transit.nextfare;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTopupRecord;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Calendar;
import java.util.TimeZone;

import static au.id.micolous.metrodroid.util.Utils.UTC;

/**
 * Represents trips on Nextfare
 */
public class NextfareTrip extends Trip implements Comparable<NextfareTrip> {
    public static final Creator<NextfareTrip> CREATOR = new Creator<NextfareTrip>() {

        public NextfareTrip createFromParcel(Parcel in) {
            return new NextfareTrip(in);
        }

        public NextfareTrip[] newArray(int size) {
            return new NextfareTrip[size];
        }
    };
    protected int mJourneyId;
    protected Mode mMode;
    protected int mModeInt;
    protected Calendar mStartTime;
    protected Calendar mEndTime;
    protected int mStartStation;
    protected int mEndStation;
    protected boolean mContinuation;
    protected int mCost;
    private String mCurrency;

    public NextfareTrip(Parcel parcel) {
        mJourneyId = parcel.readInt();
        TimeZone tz = TimeZone.getTimeZone(parcel.readString());
        mStartTime = Utils.longToCalendar(parcel.readLong(), tz);
        mEndTime = Utils.longToCalendar(parcel.readLong(), tz);
        mMode = Mode.valueOf(parcel.readString());
        mStartStation = parcel.readInt();
        mEndStation = parcel.readInt();
        mModeInt = parcel.readInt();
        mCurrency = parcel.readString();
    }

    public NextfareTrip() {
        mStartStation = -1;
        mEndStation = -1;
    }

    public NextfareTrip(@NonNull String currency) {
        this();
        mCurrency = currency;
    }

    public NextfareTrip(NextfareTopupRecord rec, @NonNull String currency) {
        mStartTime = rec.getTimestamp();
        mEndTime = null;
        mMode = Mode.TICKET_MACHINE;
        mStartStation = -1;
        mEndStation = -1;
        mModeInt = 0;
        mCost = rec.getCredit() * -1;
        mCurrency = currency;
    }

    @Override
    public Calendar getStartTimestamp() {
        return mStartTime;
    }

    @Override
    public Calendar getEndTimestamp() {
        return mEndTime;
    }

    @Override
    public Station getStartStation() {
        if (mStartStation < 0) {
            return null;
        }
        return Station.unknown(mStartStation);
    }

    @Override
    public Station getEndStation() {
        if (mEndTime != null && mEndStation > -1) {
            return Station.unknown(mEndStation);
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return new TransitCurrency(mCost, mCurrency);
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
        parcel.writeString(mStartTime == null ? UTC.getID() : mStartTime.getTimeZone().getID());
        parcel.writeLong(Utils.calendarToLong(mStartTime));
        parcel.writeLong(Utils.calendarToLong(mEndTime));
        parcel.writeString(mMode.toString());
        parcel.writeInt(mStartStation);
        parcel.writeInt(mEndStation);
        parcel.writeInt(mModeInt);
        parcel.writeString(mCurrency);
    }

    @Override
    public int compareTo(@NonNull NextfareTrip other) {
        return this.mStartTime.compareTo(other.mStartTime);
    }
}
