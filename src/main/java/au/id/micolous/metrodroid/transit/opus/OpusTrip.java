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
import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedString;
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed;
import au.id.micolous.metrodroid.transit.en1545.En1545Parser;
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

    private static final En1545Field tripFields = new En1545Bitmap(
            new En1545FixedInteger("unknownA", 8),
            new En1545FixedInteger("unknownB", 10),
            new En1545FixedInteger("EventServiceProvider", 8),
            new En1545FixedInteger("unknownC", 14),
            new En1545FixedInteger("EventRouteNumber", 16),
            new En1545FixedInteger("unknownD", 16),
            new En1545FixedInteger("unknownE", 16),
            new En1545FixedInteger("unknownF", 24),
            new En1545FixedInteger("unknownG", 24),

            // Following were never seen
            new En1545FixedInteger("unknownX1", 8),
            new En1545FixedInteger("unknownX2", 8),
            new En1545FixedInteger("unknownX3", 8),
            new En1545FixedInteger("unknownX4", 8),
            new En1545FixedInteger("unknownX5", 8),
            new En1545FixedInteger("unknownX6", 8),
            new En1545FixedInteger("unknownX7", 8),
            new En1545FixedInteger("unknownX8", 8),
            new En1545FixedInteger("unknownX9", 8),
            new En1545FixedInteger("unknownX10", 8),
            new En1545FixedInteger("unknownX11", 8),
            new En1545FixedInteger("unknownX12", 8),
            new En1545FixedInteger("unknownX13", 8),
            new En1545FixedInteger("unknownX14", 8),
            new En1545FixedInteger("unknownX15", 8),
            new En1545FixedInteger("unknownX16", 8),
            new En1545FixedInteger("unknownX17", 8),
            new En1545FixedInteger("unknownX18", 8),
            new En1545FixedInteger("unknownX19", 8)
    );
    private final En1545Parsed mParsed;

    public OpusTrip(byte[] data) {
        mParsed = En1545Parser.parse(data, 25, tripFields);
        mDay = Utils.getBitsFromBuffer(data, 0, 14);
        mTime = Utils.getBitsFromBuffer(data, 14, 11);

        mRoute = mParsed.getIntOrZero("EventRouteNumber");
        mAgency = mParsed.getIntOrZero("EventServiceProvider");
    }

    @Override
    public String getRouteName() {
        return StationTableReader.getLineName(OPUS_STR, mRoute | (mAgency << 9));
    }

    @Override
    public String getAgencyName() {
        return StationTableReader.getOperatorName(OPUS_STR, mAgency, false);
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
        mParsed.writeToParcel(dest, flags);
    }

    public OpusTrip(Parcel parcel) {
        mDay = parcel.readInt();
        mTime = parcel.readInt();
        mAgency = parcel.readInt();
        mRoute = parcel.readInt();
        mParsed = new En1545Parsed(parcel);
    }
}
