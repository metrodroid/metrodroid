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
package au.id.micolous.farebot.transit.seq_go;

import android.database.Cursor;
import android.os.Parcel;

import au.id.micolous.farebot.transit.Station;

/**
 * Implements additional fields used for Go Card stations (zone_id).
 */
public class SeqGoStation extends Station {
    public static final Creator<SeqGoStation> CREATOR = new Creator<SeqGoStation>() {
        public SeqGoStation createFromParcel(Parcel parcel) {
            return new SeqGoStation(parcel);
        }

        public SeqGoStation[] newArray(int size) {
            return new SeqGoStation[size];
        }
    };

    public SeqGoStation(Cursor cursor) {
        super(cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_NAME)),
                null,
                cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_LAT)),
                cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_LON)));
    }

    protected SeqGoStation(Parcel parcel) {
        super(parcel);
    }
}
