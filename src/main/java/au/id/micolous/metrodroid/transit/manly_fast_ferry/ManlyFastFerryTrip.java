/*
 * ManlyFastFerryTrip.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.manly_fast_ferry;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.metrodroid.transit.erg.ErgTrip;
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord;

import java.util.GregorianCalendar;

/**
 * Trips on the card are "purse debits", and it is not possible to tell it apart from non-ticket
 * usage (like cafe purchases).
 */
public class ManlyFastFerryTrip extends ErgTrip {
    public static final Parcelable.Creator<ManlyFastFerryTrip> CREATOR = new Parcelable.Creator<ManlyFastFerryTrip>() {

        public ManlyFastFerryTrip createFromParcel(Parcel in) {
            return new ManlyFastFerryTrip(in);
        }

        public ManlyFastFerryTrip[] newArray(int size) {
            return new ManlyFastFerryTrip[size];
        }
    };

    public ManlyFastFerryTrip(Parcel parcel) {
        super(parcel);
    }

    public ManlyFastFerryTrip(ErgPurseRecord purse, GregorianCalendar epoch) {
        super(purse, epoch, ManlyFastFerryTransitData.CURRENCY);
    }

    @Override
    public Mode getMode() {
        // All transactions look the same... but this is a ferry, so we'll call it a ferry one.
        // Even when you buy things at the cafe.
        return Mode.FERRY;
    }
}
