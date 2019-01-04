/*
 * CharlieCardTrip.java
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

package au.id.micolous.metrodroid.transit.charlie;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

public class CharlieCardTrip extends Trip {
    private final int mFare;
    private final int mValidator;
    private final int mTimestamp;

    public CharlieCardTrip(ImmutableByteArray data, int off) {
        mFare = CharlieCardTransitData.getPrice(data, off + 5);
        mValidator = Utils.byteArrayToInt(data, off + 3, 2);
        mTimestamp = Utils.byteArrayToInt(data, off, 3);
    }

    private CharlieCardTrip(Parcel in) {
        mFare = in.readInt();
        mValidator = in.readInt();
        mTimestamp = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mFare);
        dest.writeInt(mValidator);
        dest.writeInt(mTimestamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CharlieCardTrip> CREATOR = new Creator<CharlieCardTrip>() {
        @Override
        public CharlieCardTrip createFromParcel(Parcel in) {
            return new CharlieCardTrip(in);
        }

        @Override
        public CharlieCardTrip[] newArray(int size) {
            return new CharlieCardTrip[size];
        }
    };

    @Nullable
    @Override
    public Station getStartStation() {
        return Station.unknown(mValidator >> 3);
    }

    @Override
    public Calendar getStartTimestamp() {
        return CharlieCardTransitData.parseTimestamp(mTimestamp);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.USD(mFare);
    }

    @Override
    public Mode getMode() {
        switch (mValidator & 7) {
            case 0:
                return Mode.TICKET_MACHINE;
            case 1:
                return Mode.BUS;
        }
        return Mode.OTHER;
    }
}
