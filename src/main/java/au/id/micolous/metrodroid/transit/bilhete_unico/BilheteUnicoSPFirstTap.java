/*
 * BilheteUnicoSPFirstTap.java
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

package au.id.micolous.metrodroid.transit.bilhete_unico;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;

class BilheteUnicoSPFirstTap extends Trip {
    private final int mDay;
    private final int mTime;
    private final int mLine;

    public BilheteUnicoSPFirstTap(int day, int time, int line) {
        mDay = day;
        mTime = time;
        mLine = line;
    }

    private BilheteUnicoSPFirstTap(Parcel in) {
        mDay = in.readInt();
        mTime = in.readInt();
        mLine = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mDay);
        dest.writeInt(mTime);
        dest.writeInt(mLine);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BilheteUnicoSPFirstTap> CREATOR = new Creator<BilheteUnicoSPFirstTap>() {
        @Override
        public BilheteUnicoSPFirstTap createFromParcel(Parcel in) {
            return new BilheteUnicoSPFirstTap(in);
        }

        @Override
        public BilheteUnicoSPFirstTap[] newArray(int size) {
            return new BilheteUnicoSPFirstTap[size];
        }
    };

    @Override
    public Calendar getStartTimestamp() {
        return BilheteUnicoSPTrip.parseTimestamp(mDay, mTime);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return null;
    }

    @Override
    public Mode getMode() {
        switch (mLine >> 5) {
            case 1:
                return Mode.BUS;
            case 2:
                return Mode.TRAM;
        }
        return Mode.OTHER;
    }

    @Override
    public String getRouteName() {
        return Integer.toHexString(mLine);
    }
}
