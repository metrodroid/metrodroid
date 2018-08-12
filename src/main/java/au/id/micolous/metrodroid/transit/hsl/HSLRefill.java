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
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class HSLRefill extends Trip implements Parcelable {
    private final Calendar mRefillTime;
    private final int mRefillAmount;

    public HSLRefill(byte[] data) {
        mRefillTime = HSLTransitData.cardDateToCalendar(
                Utils.getBitsFromBuffer(data, 20, 14),
                Utils.getBitsFromBuffer(data, 34, 11));
        mRefillAmount = Utils.getBitsFromBuffer(data, 45, 20);
    }

    public HSLRefill(Parcel parcel) {
        mRefillTime = Utils.longToCalendar(parcel.readLong(), HSLTransitData.TZ);
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
        dest.writeLong(Utils.calendarToLong(mRefillTime));
        dest.writeInt(mRefillAmount);
    }

    @Override
    public Calendar getStartTimestamp() {
        return mRefillTime;
    }

    @Override
    public String getAgencyName() {
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

    @Override
    public boolean hasTime() {
        return true;
    }

}
