/*
 * ChcMetrocardTransaction.java
 *
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
package au.id.micolous.metrodroid.transit.chc_metrocard;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.GregorianCalendar;

import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.erg.ErgTransaction;
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord;

public class ChcMetrocardTransaction extends ErgTransaction {
    public static final Parcelable.Creator<ChcMetrocardTransaction> CREATOR = new Parcelable.Creator<ChcMetrocardTransaction>() {

        public ChcMetrocardTransaction createFromParcel(Parcel in) {
            return new ChcMetrocardTransaction(in);
        }

        public ChcMetrocardTransaction[] newArray(int size) {
            return new ChcMetrocardTransaction[size];
        }
    };

    private ChcMetrocardTransaction(Parcel parcel) {
        super(parcel);
    }

    public ChcMetrocardTransaction(ErgPurseRecord purse, GregorianCalendar epoch) {
        super(purse, epoch, ChcMetrocardTransitData.CURRENCY);
    }

    @Override
    public Trip.Mode getMode() {
        // There is a historic tram that circles the city, but not a commuter service, and does not
        // accept Metrocard.
        return Trip.Mode.BUS;
    }

    @Override
    protected boolean shouldBeMerged(Transaction other) {
        if (!(other instanceof ChcMetrocardTransaction)) {
            return super.shouldBeMerged(other);
        }

        final ChcMetrocardTransaction otherTxn = (ChcMetrocardTransaction) other;

        if (getTimestamp().compareTo(otherTxn.getTimestamp()) != 0) {
            // Don't merge things with different times
            return false;
        }

        if (mPurse.isCredit() && mPurse.getTransactionValue() != 0) {
            // Don't merge in top-ups.
            return false;
        }

        if (mPurse.getRoute() != otherTxn.mPurse.getRoute()) {
            // Don't merge different agency
            return false;
        }

        // Merge whe one is a trip and the other is not a trip
        return mPurse.isTrip() != otherTxn.mPurse.isTrip();
    }

    @Override
    public int compareTo(@NonNull Transaction other) {
        // This prepares ordering for a later merge
        int ret = super.compareTo(other);

        // Transactions are sorted by time alone -- but Erg transactions will have the same
        // timestamp for many things
        if (ret != 0 || !(other instanceof ChcMetrocardTransaction)) {
            return ret;
        }

        final ChcMetrocardTransaction otherTxn = (ChcMetrocardTransaction)other;

        // Put top-ups first
        if (mPurse.isCredit() && mPurse.getTransactionValue() != 0) {
            return -1;
        }

        if (otherTxn.mPurse.isCredit() && otherTxn.mPurse.getTransactionValue() != 0) {
            return 1;
        }

        // Put "trips" first
        if (mPurse.isTrip()) {
            return -1;
        }

        if (otherTxn.mPurse.isTrip()) {
            return 1;
        }

        // Finally sort by value
        return Integer.compare(mPurse.getTransactionValue(), otherTxn.mPurse.getTransactionValue());
    }
}
