/*
 * CardProvider.kt
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils

import org.jetbrains.annotations.NonNls

import au.id.micolous.farebot.BuildConfig

class CardProvider : ContentProvider() {

    private var mDbHelper: CardDBHelper? = null

    override fun onCreate(): Boolean {
        mDbHelper = CardDBHelper(context)
        return true
    }

    override fun query(@NonNls uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        @NonNls val builder = SQLiteQueryBuilder()
        when (sUriMatcher.match(uri)) {
            CardDBHelper.CARD_COLLECTION_URI_INDICATOR -> builder.tables = CardsTableColumns.TABLE_NAME
            CardDBHelper.SINGLE_CARD_URI_INDICATOR -> {
                builder.tables = CardsTableColumns.TABLE_NAME
                builder.appendWhere(CardsTableColumns._ID + " = " + uri.pathSegments[1])
            }
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }//builder.setProjectionMap();

        val db = mDbHelper!!.readableDatabase

        val cursor = builder.query(db, null, selection, selectionArgs, null, null, sortOrder)
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(@NonNls uri: Uri): String? = when (sUriMatcher.match(uri)) {
        CardDBHelper.CARD_COLLECTION_URI_INDICATOR -> CardDBHelper.CARD_DIR_TYPE
        CardDBHelper.SINGLE_CARD_URI_INDICATOR -> CardDBHelper.CARD_ITEM_TYPE
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    override fun insert(@NonNls uri: Uri, values: ContentValues?): Uri? {
        if (sUriMatcher.match(uri) != CardDBHelper.CARD_COLLECTION_URI_INDICATOR) {
            throw IllegalArgumentException("Incorrect URI: $uri")
        }

        val db = mDbHelper!!.writableDatabase
        val rowId = db.insertOrThrow(CardsTableColumns.TABLE_NAME, null, values)

        val cardUri = ContentUris.withAppendedId(CONTENT_URI_CARD, rowId)
        context!!.contentResolver.notifyChange(cardUri, null)

        return cardUri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        @NonNls val db = mDbHelper!!.writableDatabase
        var count = 0
        when (sUriMatcher.match(uri)) {
            CardDBHelper.CARD_COLLECTION_URI_INDICATOR -> count = db.delete(CardsTableColumns.TABLE_NAME, selection, selectionArgs)
            CardDBHelper.SINGLE_CARD_URI_INDICATOR -> {
                val rowId = uri.pathSegments[1]
                count = db.delete(CardsTableColumns.TABLE_NAME, CardsTableColumns._ID + "=" + rowId
                        + if (!TextUtils.isEmpty(selection)) " AND ($selection)" else "", selectionArgs)
            }
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        @NonNls val db = mDbHelper!!.writableDatabase
        val count: Int = when (sUriMatcher.match(uri)) {
            CardDBHelper.CARD_COLLECTION_URI_INDICATOR -> db.update(CardsTableColumns.TABLE_NAME, values, selection, selectionArgs)
            CardDBHelper.SINGLE_CARD_URI_INDICATOR -> {
                val rowId = uri.pathSegments[1]
                db.update(CardsTableColumns.TABLE_NAME, values, CardsTableColumns._ID + "=" + rowId
                        + if (!TextUtils.isEmpty(selection)) " AND ($selection)" else "", selectionArgs)
            }
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    companion object {
        @NonNls
        val AUTHORITY = BuildConfig.APPLICATION_ID + ".cardprovider"

        val CONTENT_URI_CARD: Uri = Uri.parse("content://$AUTHORITY/cards")

        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            sUriMatcher.addURI(AUTHORITY, "cards", CardDBHelper.CARD_COLLECTION_URI_INDICATOR)
            sUriMatcher.addURI(AUTHORITY, "cards/#", CardDBHelper.SINGLE_CARD_URI_INDICATOR)
        }
    }
}
