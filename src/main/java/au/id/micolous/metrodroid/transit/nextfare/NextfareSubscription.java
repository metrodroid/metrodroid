/*
 * NextfareSubscription.java
 *
 * Copyright 2016-2017 Michael Farrell <micolous+git@gmail.com>
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
import android.os.Parcelable;

import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareBalanceRecord;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTravelPassRecord;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Represents a Nextfare travel pass.
 */

public class NextfareSubscription extends Subscription implements Parcelable {

    public static final Creator<NextfareSubscription> CREATOR = new Creator<NextfareSubscription>() {
        @Override
        public NextfareSubscription createFromParcel(Parcel in) {
            return new NextfareSubscription(in);
        }

        @Override
        public NextfareSubscription[] newArray(int size) {
            return new NextfareSubscription[size];
        }
    };
    private Calendar mValidTo;

    public NextfareSubscription(NextfareTravelPassRecord record) {
        mValidTo = record.getTimestamp();
    }

    public NextfareSubscription(NextfareBalanceRecord record) {
        // Used when there is a subscription on the card that is not yet active.
        // TODO: Figure out subscription types
    }

    protected NextfareSubscription(Parcel in) {
        mValidTo = GregorianCalendar.getInstance();
        mValidTo.setTimeInMillis(in.readLong());
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Calendar getValidFrom() {
        return null;
    }

    @Override
    public Calendar getValidTo() {
        return mValidTo;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return "Nextfare";
    }

    @Override
    public int getMachineId() {
        return 0;
    }

    @Override
    public String getSubscriptionName() {
        return null;
    }

    @Override
    public String getActivation() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mValidTo.getTimeInMillis());
    }
}
