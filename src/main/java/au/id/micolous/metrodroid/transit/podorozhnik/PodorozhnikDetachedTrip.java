/*
 * Copyright (c) 2018 Google
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

package au.id.micolous.metrodroid.transit.podorozhnik;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.time.TimestampFormatterKt;
import au.id.micolous.metrodroid.time.TimestampFull;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;

class PodorozhnikDetachedTrip extends Trip {
    private final int mTimestamp;

    public static final Creator<PodorozhnikDetachedTrip> CREATOR = new Creator<PodorozhnikDetachedTrip>() {
        public PodorozhnikDetachedTrip createFromParcel(Parcel parcel) {
            return new PodorozhnikDetachedTrip(parcel);
        }

        public PodorozhnikDetachedTrip[] newArray(int size) {
            return new PodorozhnikDetachedTrip[size];
        }
    };

    PodorozhnikDetachedTrip(int timestamp) {
        mTimestamp = timestamp;
    }

    @Override
    public TimestampFull getStartTimestamp() {
        return TimestampFormatterKt.calendar2ts(PodorozhnikTransitData.convertDate(mTimestamp));
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return null;
    }

    @Override
    public Mode getMode() {
        return Mode.OTHER;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTimestamp);
    }

    private PodorozhnikDetachedTrip(Parcel parcel) {
        mTimestamp = parcel.readInt();
    }
}
