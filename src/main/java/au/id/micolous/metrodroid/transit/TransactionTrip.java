/*
 * TransactionTrip.java
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

package au.id.micolous.metrodroid.transit;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class TransactionTrip extends Trip implements Parcelable {
    protected Transaction mStart;
    protected Transaction mEnd;

    protected TransactionTrip(@NonNull Transaction transaction) {
        if (transaction.isTapOff() || transaction.isCancel())
            mEnd = transaction;
        else
            mStart = transaction;
    }

    protected TransactionTrip(Parcel in) {
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

    public static final Creator<TransactionTrip> CREATOR = new Creator<TransactionTrip>() {
        @Override
        public TransactionTrip createFromParcel(Parcel in) {
            return new TransactionTrip(in);
        }

        @Override
        public TransactionTrip[] newArray(int size) {
            return new TransactionTrip[size];
        }
    };

    private Transaction getAny() {
        return mStart == null ? mEnd : mStart;
    }

    @Nullable
    @Override
    public String getRouteName() {
        // Try to get the route from the nested transactions.
        // This automatically falls back to using the MdST.
        @NonNull List<String> startLines =
                mStart != null ? mStart.getRouteNames() : Collections.emptyList();
        @NonNull List<String> endLines =
                mEnd != null ? mEnd.getRouteNames() : Collections.emptyList();

        return Trip.getRouteName(startLines, endLines);
    }

    @Override
    public int getPassengerCount() {
        return getAny().getPassengerCount();
    }

    @Nullable
    @Override
    public String getVehicleID() {
        return getAny().getVehicleID();
    }

    @Nullable
    @Override
    public String getMachineID() {
        return getAny().getMachineID();
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
        if (mEnd != null && mEnd.isCancel()) {
            // No fare applies to the trip, as the tap-on was reversed.
            return null;
        }

        return getAny().getFare();
    }

    @Override
    public boolean isTransfer() {
        return getAny().isTransfer();
    }

    @Override
    public boolean isRejected() {
        return getAny().isRejected();
    }

    public interface TransactionTripFactory {
        TransactionTrip createTrip(Transaction el);
    }

    public static List<TransactionTrip> merge(List<? extends Transaction> transactions,
                                              TransactionTripFactory factory) {
        Collections.sort(transactions, (a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        List<TransactionTrip> trips = new ArrayList<>();
        for (Transaction el : transactions) {
            if (trips.isEmpty()) {
                trips.add(factory.createTrip(el));
                continue;
            }
            TransactionTrip previous = trips.get(trips.size() - 1);
            if (previous.mEnd == null && previous.mStart.shouldBeMerged(el))
                previous.mEnd = el;
            else
                trips.add(factory.createTrip(el));
        }
        return trips;
    }

    public static List<TransactionTrip> merge(List<? extends Transaction> transactions) {
        return merge(transactions, TransactionTrip::new);
    }

    public static List<TransactionTrip> merge(Transaction... transactions) {
        return merge(Arrays.asList(transactions));
    }
}
