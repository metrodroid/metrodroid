/*
 * NextfareUltralightTrip.java
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

package au.id.micolous.metrodroid.transit.nextfare.ultralight;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;

public class NextfareUltralightTrip extends Trip implements Parcelable {
    public static final Creator<NextfareUltralightTrip> CREATOR = new Creator<NextfareUltralightTrip>() {
        public NextfareUltralightTrip createFromParcel(Parcel parcel) {
            return new NextfareUltralightTrip(parcel);
        }

        public NextfareUltralightTrip[] newArray(int size) {
            return new NextfareUltralightTrip[size];
        }
    };

    private final NextfareUltralightTransaction mStart;
    private final NextfareUltralightTransaction mEnd;


    public NextfareUltralightTrip(NextfareUltralightTransaction start, NextfareUltralightTransaction end) {
        mStart = start;
        mEnd = end;
    }

    NextfareUltralightTrip(Parcel parcel) {
        int flags = parcel.readInt();
        if ((flags & 1) != 0)
            mStart = parcel.readParcelable(NextfareUltralightTransaction.class.getClassLoader());
        else
            mStart = null;
        if ((flags & 2) != 0)
            mEnd = parcel.readParcelable(NextfareUltralightTransaction.class.getClassLoader());
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
            dest.writeParcelable(mStart, flags);
        if (mEnd != null)
            dest.writeParcelable(mEnd, flags);
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
    public int describeContents() {
        return 0;
    }
}
