/*
 * ChcMetrocardTrip.java
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

import java.util.GregorianCalendar;

import au.id.micolous.metrodroid.transit.erg.ErgTrip;
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord;

public class ChcMetrocardTrip extends ErgTrip {
    public static final Parcelable.Creator<ChcMetrocardTrip> CREATOR = new Parcelable.Creator<ChcMetrocardTrip>() {

        public ChcMetrocardTrip createFromParcel(Parcel in) {
            return new ChcMetrocardTrip(in);
        }

        public ChcMetrocardTrip[] newArray(int size) {
            return new ChcMetrocardTrip[size];
        }
    };

    private ChcMetrocardTrip(Parcel parcel) {
        super(parcel);
    }

    public ChcMetrocardTrip(ErgPurseRecord purse, GregorianCalendar epoch) {
        super(purse, epoch, ChcMetrocardTransitData.CURRENCY);
    }

    @Override
    public Mode getMode() {
        // There is a historic tram that circles the city, but not a commuter service, and does not
        // accept Metrocard.
        return Mode.BUS;
    }
}
