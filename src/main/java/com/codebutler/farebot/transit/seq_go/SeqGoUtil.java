/*
 * SeqGoUtil.java
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
package com.codebutler.farebot.transit.seq_go;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import au.id.micolous.metrodroid.MetrodroidApplication;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.util.Utils;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Misc utilities for parsing Go Cards
 * @author Michael Farrell
 */
public final class SeqGoUtil {
    private static final String TAG = "SeqGoUtil";


    private static SeqGoDBUtil getDB() {
        return MetrodroidApplication.getInstance().getSeqGoDBUtil();
    }

    public static SeqGoStation getStation(int stationId) {
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
                    SeqGoDBUtil.TABLE_NAME,
                    SeqGoDBUtil.COLUMNS_STATIONDATA,
                    String.format("%s = ?", SeqGoDBUtil.COLUMN_ROW_ID),
                    new String[]{
                            String.valueOf(stationId),
                    },
                    null,
                    null,
                    SeqGoDBUtil.COLUMN_ROW_ID);

            if (!cursor.moveToFirst()) {
                Log.w(TAG, String.format("FAILED get station %s",
                        stationId));

                return null;
            }

            String stationName = cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_NAME));
            String latitude = cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_LAT));
            String longitude = cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_LON));
            String zone = cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_ZONE));
            String airtrain_zone_exempt = cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_AIRTRAIN_ZONE_EXEMPT));

            return new SeqGoStation(stationName, latitude, longitude, zone,
                    airtrain_zone_exempt != null && airtrain_zone_exempt.equals("1"));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
