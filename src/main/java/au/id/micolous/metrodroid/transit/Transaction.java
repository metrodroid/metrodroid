/*
 * TransactionTrip.java
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

package au.id.micolous.metrodroid.transit;

import android.os.Parcelable;

import java.util.Calendar;

public abstract class Transaction implements Parcelable {
    protected abstract boolean isTapOff();

    public abstract String getRouteName();

    public String getVehicleID() {
        return null;
    }

    public int getPassengerCount() {
        return -1;
    }

    public abstract String getAgencyName(boolean isShort);

    public abstract Station getStation();

    public abstract Calendar getTimestamp();

    public abstract TransitCurrency getFare();

    public abstract Trip.Mode getMode();

    protected boolean shouldBeMerged(Transaction other) {
        return isTapOn() && (other.isTapOff() || other.isCancel()) && isSameTrip(other);
    }

    protected boolean isCancel() {
        return false;
    }

    protected abstract boolean isSameTrip(Transaction other);

    protected abstract boolean isTapOn();
}
