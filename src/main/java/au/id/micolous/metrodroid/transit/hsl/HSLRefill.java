/*
 * HSLRefill.java
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.hsl;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.time.TimestampFormatterKt;
import au.id.micolous.metrodroid.time.TimestampFull;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public class HSLRefill extends Trip implements Parcelable {
    private final Calendar mRefillTime;
    private final int mRefillAmount;

    public HSLRefill(ImmutableByteArray data) {
        mRefillTime = HSLTransitData.cardDateToCalendar(
                data.getBitsFromBuffer(20, 14),
                data.getBitsFromBuffer(34, 11));
        mRefillAmount = data.getBitsFromBuffer(45, 20);
    }

    private HSLRefill(Parcel parcel) {
        mRefillTime = Utils.unparcelCalendar(parcel);
        mRefillAmount = parcel.readInt();
    }

    public static final Creator<HSLRefill> CREATOR = new Creator<HSLRefill>() {
        @Override
        public HSLRefill createFromParcel(Parcel in) {
            return new HSLRefill(in);
        }

        @Override
        public HSLRefill[] newArray(int size) {
            return new HSLRefill[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        Utils.parcelCalendar(dest, mRefillTime);
        dest.writeInt(mRefillAmount);
    }

    @Override
    public TimestampFull getStartTimestamp() {
        return TimestampFormatterKt.calendar2ts(mRefillTime);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return MetrodroidApplication.getInstance().getString(R.string.hsl_balance_refill);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.EUR(-mRefillAmount);
    }

    @Override
    public Mode getMode() {
        return Mode.TICKET_MACHINE;
    }
}
