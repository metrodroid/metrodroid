/*
 * En1545Trip.java
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

package au.id.micolous.metrodroid.transit.en1545;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;

public class En1545Trip extends Trip implements Parcelable {
    private En1545Transaction mStart;
    private En1545Transaction mEnd;

    private En1545Trip(En1545Transaction transaction) {
        if (transaction.isTapOff())
            mEnd = transaction;
        else
            mStart = transaction;
    }

    private En1545Trip(Parcel in) {
        if (in.readInt() != 0)
            mStart = in.readParcelable(getClass().getClassLoader());
        if (in.readInt() != 0)
            mEnd = in.readParcelable(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mStart != null) {
            dest.writeInt(1);
            dest.writeParcelable(mStart, flags);
        } else
            dest.writeInt(0);
        if (mEnd != null) {
            dest.writeInt(1);
            dest.writeParcelable(mEnd, flags);
        } else
            dest.writeInt(0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<En1545Trip> CREATOR = new Creator<En1545Trip>() {
        @Override
        public En1545Trip createFromParcel(Parcel in) {
            return new En1545Trip(in);
        }

        @Override
        public En1545Trip[] newArray(int size) {
            return new En1545Trip[size];
        }
    };

    private En1545Transaction getAny() {
        return mStart == null ? mEnd : mStart;
    }

    @Override
    public String getRouteName() {
        return getAny().getRouteName();
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return getAny().getAgencyName(isShort);
    }

    @Nullable
    @Override
    public Station getStartStation() {
        if (mStart == null)
            return null;
        return mStart.getStation();
    }

    @Nullable
    @Override
    public Station getEndStation() {
        if (mEnd == null)
            return null;
        return mEnd.getStation();
    }

    @Override
    public Calendar getStartTimestamp() {
        if (mStart == null)
            return null;
        return mStart.getTimestamp();
    }

    @Override
    public Calendar getEndTimestamp() {
        if (mEnd == null)
            return null;
        return mEnd.getTimestamp();
    }

    @Override
    public Mode getMode() {
        return getAny().getMode();
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return getAny().getFare();
    }

    public static List<En1545Trip> merge(List<En1545Transaction> transactions) {
        Collections.sort(transactions, (a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        List<En1545Trip> trips = new ArrayList<>();
        for (En1545Transaction el : transactions) {
            if (trips.isEmpty()) {
                trips.add(new En1545Trip(el));
                continue;
            }
            En1545Trip previous = trips.get(trips.size() - 1);
            if (previous.mEnd == null && previous.mStart.shouldBeMerged(el))
                previous.mEnd = el;
            else
                trips.add(new En1545Trip(el));
        }
        return trips;
    }
}
