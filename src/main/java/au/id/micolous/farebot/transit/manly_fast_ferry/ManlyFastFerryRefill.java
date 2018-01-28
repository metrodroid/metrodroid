/*
 * ManlyFastFerryRefill.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.farebot.transit.manly_fast_ferry;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.farebot.transit.Refill;
import au.id.micolous.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Describes top-up amounts "purse credits".
 */
public class ManlyFastFerryRefill extends Refill {
    public static final Parcelable.Creator<ManlyFastFerryRefill> CREATOR = new Parcelable.Creator<ManlyFastFerryRefill>() {

        public ManlyFastFerryRefill createFromParcel(Parcel in) {
            return new ManlyFastFerryRefill(in);
        }

        public ManlyFastFerryRefill[] newArray(int size) {
            return new ManlyFastFerryRefill[size];
        }
    };
    private GregorianCalendar mEpoch;
    private ManlyFastFerryPurseRecord mPurse;

    public ManlyFastFerryRefill(ManlyFastFerryPurseRecord purse, GregorianCalendar epoch) {
        mPurse = purse;
        mEpoch = epoch;
    }

    public ManlyFastFerryRefill(Parcel parcel) {
        mPurse = new ManlyFastFerryPurseRecord(parcel);
        mEpoch = new GregorianCalendar();
        mEpoch.setTimeInMillis(parcel.readLong());
    }

    @Override
    public long getTimestamp() {
        GregorianCalendar ts = new GregorianCalendar();
        ts.setTimeInMillis(mEpoch.getTimeInMillis());
        ts.add(Calendar.DATE, mPurse.getDay());
        ts.add(Calendar.MINUTE, mPurse.getMinute());

        return ts.getTimeInMillis() / 1000;
    }

    @Override
    public String getAgencyName() {
        // There is only one agency on the card, don't show anything.
        return null;
    }

    @Override
    public String getShortAgencyName() {
        // There is only one agency on the card, don't show anything.
        return null;
    }

    @Override
    public int getAmount() {
        return mPurse.getTransactionValue();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        mPurse.writeToParcel(parcel, i);
        parcel.writeLong(mEpoch.getTimeInMillis());
    }
}
