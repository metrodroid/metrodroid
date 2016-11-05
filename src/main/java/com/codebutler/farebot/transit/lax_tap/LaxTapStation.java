package com.codebutler.farebot.transit.lax_tap;

import android.database.Cursor;
import android.os.Parcel;

import com.codebutler.farebot.transit.Station;

/**
 * Implements fields used by LAX TAP.
 */
public class LaxTapStation extends Station {

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
}
