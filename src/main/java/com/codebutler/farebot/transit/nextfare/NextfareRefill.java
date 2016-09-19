/*
 * NextfareRefill.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package com.codebutler.farebot.transit.nextfare;

import android.os.Parcel;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.nextfare.record.NextfareTopupRecord;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Represents a top-up event on Nextfare.
 */
public class NextfareRefill extends Refill {
    protected NextfareTopupRecord mTopup;

    public NextfareRefill(NextfareTopupRecord topup) {
        mTopup = topup;
    }

    @Override
    public long getTimestamp() {
        return mTopup.getTimestamp().getTimeInMillis() / 1000;
    }

    @Override
    public String getAgencyName() {
        return null;
    }

    @Override
    public String getShortAgencyName() {
        return null;
    }

    @Override
    public long getAmount() {
        return mTopup.getCredit();
    }

    @Override
    public String getAmountString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)getAmount() / 100);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        mTopup.writeToParcel(parcel, i);
    }

    public NextfareRefill(Parcel parcel) {
        mTopup = new NextfareTopupRecord(parcel);
    }

    public static final Creator<NextfareRefill> CREATOR = new Creator<NextfareRefill>() {

        public NextfareRefill createFromParcel(Parcel in) {
            return new NextfareRefill(in);
        }

        public NextfareRefill[] newArray(int size) {
            return new NextfareRefill[size];
        }
    };
}
