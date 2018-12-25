/*
 * ClipperRefill.java
 *
 * Copyright 2011 "an anonymous contributor"
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
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
package au.id.micolous.metrodroid.transit.clipper;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class ClipperRefill extends Trip implements Comparable<ClipperRefill> {
    public static final Creator<ClipperRefill> CREATOR = new Creator<ClipperRefill>() {
        public ClipperRefill createFromParcel(Parcel parcel) {
            return new ClipperRefill(parcel);
        }

        public ClipperRefill[] newArray(int size) {
            return new ClipperRefill[size];
        }
    };
    private final Calendar mTimestamp;
    private final int mAmount;
    private final String mMachineID;
    private final int mAgency;

    public ClipperRefill(Calendar timestamp, int amount, int agency, String machineid) {
        // NOTE: All data must be in CLIPPER_TZ.
        mTimestamp = timestamp;
        mAmount = amount;
        mMachineID = machineid;
        mAgency = agency;
    }

    public ClipperRefill(Parcel parcel) {
        mTimestamp = Utils.unparcelCalendar(parcel);
        mAmount = parcel.readInt();
        mMachineID = parcel.readString();
        mAgency = parcel.readInt();
    }

    @Override
    public String getMachineID() {
        return mMachineID;
    }

    @Override
    public Calendar getStartTimestamp() {
        Log.d("rts", Long.toString(mTimestamp.getTimeInMillis()));
        return mTimestamp;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return ClipperData.getAgencyName(mAgency, isShort);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.USD(-mAmount);
    }

    @Override
    public Mode getMode() {
        return Mode.TICKET_MACHINE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        Utils.parcelCalendar(parcel, mTimestamp);
        parcel.writeInt(mAmount);
        parcel.writeString(mMachineID);
        parcel.writeInt(mAgency);
    }

    @Override
    public int compareTo(@NonNull ClipperRefill other) {
        return this.mTimestamp.compareTo(other.mTimestamp);
    }
}
