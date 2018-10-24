/*
 * NewShenzhenTrip.java
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

package au.id.micolous.metrodroid.transit.china;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class ChinaTrip extends Trip {
    private final long mTime;
    private final int mCost;
    protected final int mType;
    protected final long mStation;

    protected ChinaTrip(Parcel parcel) {
        mTime = parcel.readLong();
        mCost = parcel.readInt();
        mType = parcel.readInt();
        mStation = parcel.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mTime);
        parcel.writeInt(mCost);
        parcel.writeInt(mType);
        parcel.writeLong(mStation);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.CNY(isTopup() ? -mCost : mCost);
    }

    protected boolean isTopup() {
        return mType == 2;
    }

    protected int getTransport() {
        return (int)(mStation >> 28);
    }

    public Calendar getTimestamp() {
        return NewShenzhenTransitData.parseHexDateTime (mTime);
    }

    public boolean isValid() {
        return (mCost != 0 || mTime != 0);
    }

    // Should be overridden if anything is known about transports
    @Override
    public Mode getMode() {
        if (isTopup())
            return Mode.TICKET_MACHINE;
        return Mode.OTHER;
    }

    // Should be overridden if anything is known about transports
    @Override
    public String getRouteName() {
        return Long.toHexString(mStation) + "/" + mType;
    }

    @Override
    public Calendar getStartTimestamp() {
        return getTimestamp();
    }

    protected ChinaTrip(byte[] data) {
        // 2 bytes counter
        // 3 bytes zero
        // 4 bytes cost
        mCost = Utils.byteArrayToInt(data, 5,4);
        mType = data[9];
        mStation = Utils.byteArrayToLong(data, 10, 6);
        mTime = Utils.byteArrayToLong(data, 16, 7);
    }
}
