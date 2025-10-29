/*
 * KeysDBHelper.kt
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package au.id.micolous.metrodroid.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import org.jetbrains.annotations.NonNls

import au.id.micolous.farebot.BuildConfig

class KeysDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE " + KeysTableColumns.TABLE_NAME + " ("
                + KeysTableColumns._ID + " INTEGER PRIMARY KEY, "
                + KeysTableColumns.CARD_ID + " TEXT NOT NULL, "
                + KeysTableColumns.CARD_TYPE + " TEXT NOT NULL, "
                + KeysTableColumns.KEY_DATA + " BLOB NOT NULL, "
                + KeysTableColumns.CREATED_AT + " LONG NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {} // Not Implemented...

    companion object {
        @NonNls
        val KEY_DIR_TYPE = "vnd.android.cursor.dir/" + BuildConfig.APPLICATION_ID + ".key"
        @NonNls
        val KEY_ITEM_TYPE = "vnd.android.cursor.item/" + BuildConfig.APPLICATION_ID + ".key"
        @NonNls
        private const val DATABASE_NAME = "keys.db"
        private const val DATABASE_VERSION = 3
    }
}
