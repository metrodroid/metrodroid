/*
 * UnauthorizedTransitData.java
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
package au.id.micolous.metrodroid.transit.unknown;

import android.os.Parcel;

import au.id.micolous.metrodroid.transit.TransitData;

/**
 * Base class for all types of cards where we are unable to read any useful data (without a key).
 */
public abstract class UnauthorizedTransitData extends TransitData {

    @Override
    public String getSerialNumber() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }
}
