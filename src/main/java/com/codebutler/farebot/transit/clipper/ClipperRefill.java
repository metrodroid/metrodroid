/*
 * ClipperRefill.java
 *
 * Copyright 2011 "an anonymous contributor"
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
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
package com.codebutler.farebot.transit.clipper;

import android.os.Parcel;

import com.codebutler.farebot.transit.Refill;

public class ClipperRefill extends Refill {
    public static final Creator<ClipperRefill> CREATOR = new Creator<ClipperRefill>() {
        public ClipperRefill createFromParcel(Parcel parcel) {
            return new ClipperRefill(parcel);
        }

        public ClipperRefill[] newArray(int size) {
            return new ClipperRefill[size];
        }
    };
    final long mTimestamp;
    final int mAmount;
    final long mMachineID;
    final long mAgency;

    public ClipperRefill(long timestamp, int amount, long agency, long machineid) {
        mTimestamp = timestamp;
        mAmount = amount;
        mMachineID = machineid;
        mAgency = agency;
    }

    public ClipperRefill(Parcel parcel) {
        mTimestamp = parcel.readLong();
        mAmount = parcel.readInt();
        mMachineID = parcel.readLong();
        mAgency = parcel.readLong();
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public int getAmount() {
        return mAmount;
    }

    public long getMachineID() {
        return mMachineID;
    }

    @Override
    public String getAgencyName() {
        return ClipperTransitData.getAgencyName((int) mAgency);
    }

    @Override
    public String getShortAgencyName() {
        return ClipperTransitData.getShortAgencyName((int) mAgency);
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mTimestamp);
        parcel.writeInt(mAmount);
        parcel.writeLong(mMachineID);
        parcel.writeLong(mAgency);
    }
}
