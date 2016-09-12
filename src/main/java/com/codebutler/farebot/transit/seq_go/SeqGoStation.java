/*
 * SeqGoRefill.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
package com.codebutler.farebot.transit.seq_go;

import android.os.Parcel;

import com.codebutler.farebot.transit.Station;

/**
 * Implements additional fields used for Go Card stations (zone_id).
 *
 */
public class SeqGoStation extends Station {
    protected final String mZone;
    protected final boolean mAirtrainZoneExempt;

    public SeqGoStation(String stationName, String latitude, String longitude, String zone, boolean airtrain_zone_exempt) {
        super(stationName, null, latitude, longitude);
        this.mZone = zone;
        this.mAirtrainZoneExempt = airtrain_zone_exempt;
    }

    public String getZone() {
        return this.mZone;
    }

    public boolean isAirtrainZoneExempt() {
        return mAirtrainZoneExempt;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(mZone);
        parcel.writeInt(mAirtrainZoneExempt ? 1 : 0);
    }

    protected SeqGoStation(Parcel parcel) {
        super(parcel);
        this.mZone = parcel.readString();
        this.mAirtrainZoneExempt = parcel.readInt() == 1;
    }

    public static final Creator<SeqGoStation> CREATOR = new Creator<SeqGoStation>() {
        public SeqGoStation createFromParcel(Parcel parcel) {
            return new SeqGoStation(parcel);
        }
        public SeqGoStation[] newArray(int size) {
            return new SeqGoStation[size];
        }
    };
}
