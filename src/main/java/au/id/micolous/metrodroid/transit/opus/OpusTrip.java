/*
 * OpusTrip.java
 *
 * Copyright 2018 Etienne Dubeau
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

package au.id.micolous.metrodroid.transit.opus;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

class OpusTrip extends Trip {
    private final int mDay;
    private final int mTime;
    private final int mAgency;
    private final int mRoute;
    public static final String OPUS_STR = "opus";

    public static final Creator<OpusTrip> CREATOR = new Creator<OpusTrip>() {
        public OpusTrip createFromParcel(Parcel parcel) {
            return new OpusTrip(parcel);
        }

        public OpusTrip[] newArray(int size) {
            return new OpusTrip[size];
        }
    };

    public OpusTrip(byte[] data) {
        // copied from LecteurOpus
        mDay = Utils.getBitsFromBuffer(data, 0, 14);
        mTime = Utils.getBitsFromBuffer(data, 14, 11);

        int offset = 0;
        if(Utils.getBitsFromBuffer(data, 44, 12) == 0xFF8) {
            offset = 8;
        }

        mRoute  = Utils.getBitsFromBuffer(data, 92 + offset, 9);
        mAgency = Utils.getBitsFromBuffer(data, 63 + offset, 8);
        // end of copy
    }

    @Override
    public String getRouteName() {
        return StationTableReader.getLineName(OPUS_STR, mRoute | (mAgency << 9));
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return StationTableReader.getOperatorName(OPUS_STR, mAgency, isShort);
    }

    @Override
    public Calendar getStartTimestamp() {
        return OpusTransitData.parseTime(mDay, mTime);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return null;
    }

    @Override
    public Mode getMode() {
        Mode mode;
        mode = StationTableReader.getLineMode(OPUS_STR, mRoute | (mAgency << 9));
        if (mode != null)
            return mode;
        mode = StationTableReader.getOperatorDefaultMode(OPUS_STR, mAgency);
        if (mode != null)
            return mode;
        return Mode.OTHER;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mDay);
        dest.writeInt(mTime);
        dest.writeInt(mAgency);
        dest.writeInt(mRoute);
    }

    public OpusTrip(Parcel parcel) {
        mDay = parcel.readInt();
        mTime = parcel.readInt();
        mAgency = parcel.readInt();
        mRoute = parcel.readInt();
    }
}
