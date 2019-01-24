/*
 * ClipperUltralightTrip.java
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

package au.id.micolous.metrodroid.transit.clipper;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public class ClipperUltralightTrip extends Trip {
    private final int mTime;
    private final int mTransferExpiry;
    private final int mSeqCounter;
    private final int mTripsRemaining;
    private final int mBalanceSeqCounter;
    private final int mStation;
    private final int mType;
    private final int mAgency;

    public ClipperUltralightTrip(ImmutableByteArray transaction, int baseDate) {
        mSeqCounter = transaction.getBitsFromBuffer(0, 7);
        mType = transaction.getBitsFromBuffer(7, 17);
        mTime = baseDate * 1440 - transaction.getBitsFromBuffer(24, 17);
        mStation = transaction.getBitsFromBuffer(41, 17);
        mAgency = transaction.getBitsFromBuffer(68, 5);
        mBalanceSeqCounter = transaction.getBitsFromBuffer(80, 4);
        mTripsRemaining = transaction.getBitsFromBuffer(84, 6);
        mTransferExpiry = transaction.getBitsFromBuffer(100, 10);
        // Last 4 bytes are hash
    }

    public boolean isHidden() {
        return (mType == 1);
    }

    private ClipperUltralightTrip(Parcel in) {
        mTime = in.readInt();
        mType = in.readInt();
        mTransferExpiry = in.readInt();
        mSeqCounter = in.readInt();
        mTripsRemaining = in.readInt();
        mBalanceSeqCounter = in.readInt();
        mStation = in.readInt();
        mAgency = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTime);
        dest.writeInt(mType);
        dest.writeInt(mTransferExpiry);
        dest.writeInt(mSeqCounter);
        dest.writeInt(mTripsRemaining);
        dest.writeInt(mBalanceSeqCounter);
        dest.writeInt(mStation);
        dest.writeInt(mAgency);
    }

    @Nullable
    @Override
    public Station getStartStation() {
        return ClipperData.getStation(mAgency, mStation, false);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ClipperUltralightTrip> CREATOR = new Creator<ClipperUltralightTrip>() {
        @Override
        public ClipperUltralightTrip createFromParcel(Parcel in) {
            return new ClipperUltralightTrip(in);
        }

        @Override
        public ClipperUltralightTrip[] newArray(int size) {
            return new ClipperUltralightTrip[size];
        }
    };

    @Override
    public Calendar getStartTimestamp() {
        return ClipperTransitData.clipperTimestampToCalendar(mTime * 60L);
    }

    public int getTripsRemaining() {
        return mTripsRemaining;
    }

    public int getTransferExpiry() {
        if (mTransferExpiry == 0)
            return 0;
        return mTransferExpiry + mTime;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return null;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return ClipperData.getAgencyName(mAgency, isShort);
    }

    @Override
    public Mode getMode() {
        return ClipperData.getMode(mAgency);
    }

    public boolean isSeqGreater(ClipperUltralightTrip other) {
        if (other.mBalanceSeqCounter != mBalanceSeqCounter)
            return ((mBalanceSeqCounter - other.mBalanceSeqCounter) & 0x8) == 0;
        return ((mSeqCounter - other.mSeqCounter) & 0x40) == 0;
    }
}
