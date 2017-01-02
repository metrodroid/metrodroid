/*
 * LaxTapDBUtil.java
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
package com.codebutler.farebot.transit.lax_tap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.codebutler.farebot.util.DBUtil;

import java.io.IOException;

import au.id.micolous.metrodroid.MetrodroidApplication;

/**
 * Database functionality for Los Angeles TAP cards
 */
public class LaxTapDBUtil extends DBUtil {
    public static final String TABLE_NAME = "stops";
    public static final String COLUMN_ROW_ID = "id";
    public static final String COLUMN_ROW_NAME = "name";
    public static final String COLUMN_ROW_LON = "x";
    public static final String COLUMN_ROW_LAT = "y";
    public static final String COLUMN_ROW_AGENCY = "agency_id";
    public static final String[] COLUMNS_STATIONDATA = {
            COLUMN_ROW_ID,
            COLUMN_ROW_AGENCY,
            COLUMN_ROW_NAME,
            COLUMN_ROW_LON,
            COLUMN_ROW_LAT,
    };
    private static final String TAG = "LaxTapDBUtil";
    private static final String DB_NAME = "lax_tap_stations.db3";

    private static final int VERSION = 3975;

    public LaxTapDBUtil(Context context) {
        super(context);
    }

    public static LaxTapStation getStation(int stationId, int agencyId) {
        if (stationId == 0) {
            return null;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            try {
                db = getDB().openDatabase();
            } catch (IOException ex) {
                Log.e(TAG, "Error connecting database", ex);
                return null;
            }

            cursor = db.query(
                    LaxTapDBUtil.TABLE_NAME,
                    LaxTapDBUtil.COLUMNS_STATIONDATA,
                    String.format("%s = ? AND %s = ?", LaxTapDBUtil.COLUMN_ROW_ID, LaxTapDBUtil.COLUMN_ROW_AGENCY),
                    new String[]{
                            String.valueOf(stationId),
                            String.valueOf(agencyId),
                    },
                    null,
                    null,
                    LaxTapDBUtil.COLUMN_ROW_ID);

            if (!cursor.moveToFirst()) {
                Log.w(TAG, String.format("FAILED get station %s",
                        stationId));

                return null;
            }

            return new LaxTapStation(cursor, agencyId);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static LaxTapDBUtil getDB() {
        return MetrodroidApplication.getInstance().getLaxTapDBUtil();
    }

    @Override
    protected String getDBName() {
        return DB_NAME;
    }

    @Override
    protected int getDesiredVersion() {
        return VERSION;
    }
}
