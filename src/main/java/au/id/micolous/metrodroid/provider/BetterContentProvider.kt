/*
 * BetterContentProvider.kt
 *
 * Copyright (C) 2012 Eric Butler
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
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils

import org.jetbrains.annotations.NonNls

abstract class BetterContentProvider(private val mHelperClass: Class<out SQLiteOpenHelper>, private val mDirType: String, private val mItemType: String,
                                     private val mTableName: String, private val mContentUri: Uri) : ContentProvider() {
    private var mHelper: SQLiteOpenHelper? = null
    private val mUriMatcher: UriMatcher

    init {
        val basePath = mContentUri.path!!.substring(1)

        mUriMatcher = createUriMatcher(mContentUri, basePath)
    }

    override fun onCreate(): Boolean {
        try {
            mHelper = mHelperClass.getConstructor(Context::class.java).newInstance(context)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?,
                       sortOrder: String?): Cursor? {
        val builder = SQLiteQueryBuilder()
        builder.tables = mTableName
        appendWheres(builder, mUriMatcher, uri)

        val db = mHelper!!.readableDatabase

        val cursor = builder.query(db, null, selection, selectionArgs, null, null, sortOrder)
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(@NonNls uri: Uri): String? = when (mUriMatcher.match(uri)) {
        CODE_COLLECTION -> mDirType
        CODE_SINGLE -> mItemType
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    override fun insert(@NonNls uri: Uri, values: ContentValues?): Uri? {
        if (mUriMatcher.match(uri) != CODE_COLLECTION) {
            throw IllegalArgumentException("Incorrect URI: $uri")
        }

        val db = mHelper!!.writableDatabase
        val rowId = db.insertOrThrow(mTableName, null, values)

        val itemUri = ContentUris.withAppendedId(mContentUri, rowId)
        context!!.contentResolver.notifyChange(itemUri, null)

        return itemUri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        @NonNls val db = mHelper!!.writableDatabase
        val count: Int = when (mUriMatcher.match(uri)) {
            CODE_SINGLE -> {
                val rowId = uri.pathSegments[1]
                if (TextUtils.isEmpty(selection)) {
                    db.delete(mTableName, BaseColumns._ID + "=?", arrayOf(rowId))
                } else {
                    db.delete(mTableName,
                            "$selection AND ${BaseColumns._ID}=$rowId",
                            selectionArgs)
                }
            }
            CODE_COLLECTION -> db.delete(mTableName, selection, selectionArgs)
            else -> 0
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    override fun update(@NonNls uri: Uri, values: ContentValues?, @NonNls selection: String?, selectionArgs: Array<String>?): Int {
        @NonNls val db = mHelper!!.writableDatabase
        val count: Int = when (mUriMatcher.match(uri)) {
            CODE_COLLECTION -> db.update(mTableName, values, selection, selectionArgs)
            CODE_SINGLE -> {
                val rowId = uri.pathSegments[1]
                if (TextUtils.isEmpty(selection)) {
                    db.update(mTableName, values, "${BaseColumns._ID}=$rowId", null)
                } else {
                    db.update(mTableName, values,
                            "$selection AND ${BaseColumns._ID}=$rowId",
                            selectionArgs)
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    protected open fun createUriMatcher(contentUri: Uri, @NonNls basePath: String): UriMatcher {
        val matcher = UriMatcher(UriMatcher.NO_MATCH)
        matcher.addURI(contentUri.authority, basePath, CODE_COLLECTION)
        matcher.addURI(contentUri.authority, "$basePath/#", CODE_SINGLE)
        return matcher
    }

    protected open fun appendWheres(builder: SQLiteQueryBuilder, matcher: UriMatcher, uri: Uri) {
        when (matcher.match(uri)) {
            CODE_COLLECTION -> {
            }
            CODE_SINGLE ->
                builder.appendWhere(BaseColumns._ID + "=" + uri.pathSegments[1])
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }// Nothing needed here
    }

    companion object {
        protected const val CODE_COLLECTION = 100
        protected const val CODE_SINGLE = 101
    }
}
