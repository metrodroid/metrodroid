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
package au.id.micolous.metrodroid.transit.nextfare;

import android.os.Parcel;

import au.id.micolous.metrodroid.transit.Refill;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTopupRecord;

/**
 * Represents a top-up event on Nextfare.
 */
public class NextfareRefill extends Refill {
    public static final Creator<NextfareRefill> CREATOR = new Creator<NextfareRefill>() {

        public NextfareRefill createFromParcel(Parcel in) {
            return new NextfareRefill(in);
        }

        public NextfareRefill[] newArray(int size) {
            return new NextfareRefill[size];
        }
    };
    protected NextfareTopupRecord mTopup;

    public NextfareRefill(NextfareTopupRecord topup) {
        mTopup = topup;
    }

    public NextfareRefill(Parcel parcel) {
        mTopup = new NextfareTopupRecord(parcel);
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
    public int getAmount() {
        return mTopup.getCredit();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        mTopup.writeToParcel(parcel, i);
    }
}
