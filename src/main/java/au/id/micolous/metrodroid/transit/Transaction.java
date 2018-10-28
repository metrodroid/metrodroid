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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public abstract class Transaction implements Parcelable {
    protected abstract boolean isTapOff();

    /**
     * This method may be overridden to provide candidate line names associated with the
     * transaction. This is useful if there is a separate field on the card which encodes the route
     * or line taken, and that knowledge of the station alone is not generally sufficient to
     * determine the correct route.
     *
     * By default, this gets candidate route names from the Station.
     */
    @NonNull
    public List<String> getRouteNames() {
        Station s = getStation();
        return s != null ? s.getLineNames() : Collections.emptyList();
    }

    public String getVehicleID() {
        return null;
    }

    public String getMachineID() { return null; }

    public int getPassengerCount() {
        return -1;
    }

    @Nullable
    public String getAgencyName(boolean isShort) {
        return null;
    }

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

    protected boolean isTransfer() {
        return false;
    }

    protected boolean isRejected() {
        return false;
    }
}
