/*
 * BilheteUnicoSPRefilljava
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

class BilheteUnicoSPRefill extends Trip {
    private final int mDay;
    private final int mAmount;

    @Override
    public boolean hasTime() {
        return false;
    }

    public BilheteUnicoSPRefill(int day, int amount) {
        mDay = day;
        mAmount = amount;
    }

    private BilheteUnicoSPRefill(Parcel in) {
        mDay = in.readInt();
        mAmount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mDay);
        dest.writeInt(mAmount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BilheteUnicoSPRefill> CREATOR = new Creator<BilheteUnicoSPRefill>() {
        @Override
        public BilheteUnicoSPRefill createFromParcel(Parcel in) {
            return new BilheteUnicoSPRefill(in);
        }

        @Override
        public BilheteUnicoSPRefill[] newArray(int size) {
            return new BilheteUnicoSPRefill[size];
        }
    };

    @Override
    public Calendar getStartTimestamp() {
        return BilheteUnicoSPTrip.parseTimestamp(mDay, 0);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return new TransitCurrency(-mAmount, "BRL");
    }

    @Override
    public Mode getMode() {
        return Mode.TICKET_MACHINE;
    }
}
