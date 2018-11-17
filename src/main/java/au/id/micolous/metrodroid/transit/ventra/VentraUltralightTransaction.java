/*
 * VentraUltralightTrip.java
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

package au.id.micolous.metrodroid.transit.ventra;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.TimeZone;

import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.nextfare.ultralight.NextfareUltralightTransaction;

public class VentraUltralightTransaction extends NextfareUltralightTransaction implements Parcelable {
    public static final Parcelable.Creator<VentraUltralightTransaction> CREATOR = new Parcelable.Creator<VentraUltralightTransaction>() {
        public VentraUltralightTransaction createFromParcel(Parcel parcel) {
            return new VentraUltralightTransaction(parcel);
        }

        public VentraUltralightTransaction[] newArray(int size) {
            return new VentraUltralightTransaction[size];
        }
    };

    VentraUltralightTransaction(UltralightCard card, int startPage, int baseDate) {
        super(card, startPage, baseDate);
    }

    private VentraUltralightTransaction(Parcel parcel) {
        super(parcel);
    }

    @Override
    protected TimeZone getTimezone() {
        return VentraUltralightTransitData.TZ;
    }

    protected boolean isBus() {
        return false;
    }

    public Trip.Mode getMode() {
        if (isBus())
            return Trip.Mode.BUS;
        if (mRoute == 0)
            return Trip.Mode.TICKET_MACHINE;
        return Trip.Mode.OTHER;
    }
}
