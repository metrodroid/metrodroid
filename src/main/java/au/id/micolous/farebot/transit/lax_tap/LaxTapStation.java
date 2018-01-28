/*
 * LaxTapStation.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.farebot.transit.lax_tap;

import android.database.Cursor;
import android.os.Parcel;

import au.id.micolous.farebot.transit.Station;

/**
 * Implements fields used by LAX TAP.
 */
public class LaxTapStation extends Station {

    public static final Creator<LaxTapStation> CREATOR = new Creator<LaxTapStation>() {
        @Override
        public LaxTapStation createFromParcel(Parcel in) {
            return new LaxTapStation(in);
        }

        @Override
        public LaxTapStation[] newArray(int size) {
            return new LaxTapStation[size];
        }
    };


    public LaxTapStation(Cursor cursor, int agencyId) {
        super(
                LaxTapData.AGENCIES.get(agencyId, null),
                null,
                cursor.getString(cursor.getColumnIndex(LaxTapDBUtil.COLUMN_ROW_NAME)),
                null,
                cursor.getString(cursor.getColumnIndex(LaxTapDBUtil.COLUMN_ROW_LAT)),
                cursor.getString(cursor.getColumnIndex(LaxTapDBUtil.COLUMN_ROW_LON)));


    }

    protected LaxTapStation(Parcel parcel) {
        super(parcel);
    }
}
