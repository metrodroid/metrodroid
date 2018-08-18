/*
 * CompassUltralightTrip.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.compass;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;

public class CompassUltralightTrip extends Trip implements Parcelable {
    public static final Creator<CompassUltralightTrip> CREATOR = new Creator<CompassUltralightTrip>() {
        public CompassUltralightTrip createFromParcel(Parcel parcel) {
            return new CompassUltralightTrip(parcel);
        }

        public CompassUltralightTrip[] newArray(int size) {
            return new CompassUltralightTrip[size];
        }
    };

    private final CompassUltralightTransaction mStart;
    private final CompassUltralightTransaction mEnd;


    public CompassUltralightTrip(CompassUltralightTransaction start, CompassUltralightTransaction end) {
        mStart = start;
        mEnd = end;
    }

    CompassUltralightTrip(Parcel parcel) {
        int flags = parcel.readInt();
        if ((flags & 1) != 0)
            mStart = new CompassUltralightTransaction(parcel);
        else
            mStart = null;
        if ((flags & 2) != 0)
            mEnd = new CompassUltralightTransaction(parcel);
        else
            mEnd = null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int flg = 0;
        if (mStart != null) {
            flg |= 1;
        }
        if (mEnd != null) {
            flg |= 2;
        }
        dest.writeInt(flg);
        if (mStart != null)
            mStart.writeToParcel(dest, flags);
        if (mEnd != null)
            mEnd.writeToParcel(dest, flags);
    }

    @Override
    public String getRouteName() {
        if (mStart != null)
            return mStart.getRouteName();
        if (mEnd != null)
            return mEnd.getRouteName();
        return null;
    }

    @Override
    public Station getStartStation() {
        if (mStart != null)
            return mStart.getStation();
        return null;
    }

    @Override
    public Station getEndStation() {
        if (mEnd != null)
            return mEnd.getStation();
        return null;
    }

    @Override
    public Calendar getStartTimestamp() {
        if (mStart != null)
            return mStart.getTimestamp();
        return null;
    }

    @Override
    public Calendar getEndTimestamp() {
        if (mEnd != null)
            return mEnd.getTimestamp();
        return null;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return null;
    }

    @Override
    public Mode getMode() {
        if(mStart != null)
            return mStart.getMode();
        if(mEnd != null)
            return mEnd.getMode();
        return Mode.OTHER;
    }

    @Override
    public boolean hasTime() {
        return getStartTimestamp() != null || getEndTimestamp() != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
