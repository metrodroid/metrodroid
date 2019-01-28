/*
 * CompassUltralightTrip.java
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

package au.id.micolous.metrodroid.transit.yvr_compass;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.TimeZone;

import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.nextfare.ultralight.NextfareUltralightTransaction;
import au.id.micolous.metrodroid.util.StationTableReader;

public class CompassUltralightTransaction extends NextfareUltralightTransaction implements Parcelable {
    public static final Parcelable.Creator<CompassUltralightTransaction> CREATOR = new Parcelable.Creator<CompassUltralightTransaction>() {
        public CompassUltralightTransaction createFromParcel(Parcel parcel) {
            return new CompassUltralightTransaction(parcel);
        }

        public CompassUltralightTransaction[] newArray(int size) {
            return new CompassUltralightTransaction[size];
        }
    };
    private static final String COMPASS_STR = "compass";

    CompassUltralightTransaction(UltralightCard card, int startPage, int baseDate) {
        super(card, startPage, baseDate);
    }

    private CompassUltralightTransaction(Parcel parcel) {
        super(parcel);
    }

    public Station getStation() {
        if (mLocation == 0)
            return null;
        return StationTableReader.Companion.getStation(COMPASS_STR, mLocation);
    }

    @Override
    protected TimeZone getTimezone() {
        return CompassUltralightTransitData.TZ;
    }

    protected boolean isBus() {
        return mRoute == 5 || mRoute == 7;
    }

    public Trip.Mode getMode() {
        if (isBus())
            return Trip.Mode.BUS;
        if (mRoute == 3 || mRoute == 9 || mRoute == 0xa)
            return Trip.Mode.TRAIN;
        if (mRoute == 0)
            return Trip.Mode.TICKET_MACHINE;
        return Trip.Mode.OTHER;
    }
}
