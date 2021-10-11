/*
 * CardKeyProvider.kt
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

import android.content.ContentValues
import android.content.UriMatcher
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri

import org.jetbrains.annotations.NonNls

import java.util.GregorianCalendar

import au.id.micolous.farebot.BuildConfig

class CardKeyProvider : BetterContentProvider(KeysDBHelper::class.java,
        KeysDBHelper.KEY_DIR_TYPE,
        KeysDBHelper.KEY_ITEM_TYPE,
        KeysTableColumns.TABLE_NAME, CONTENT_URI) {

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val now = GregorianCalendar.getInstance().timeInMillis
        values!!.put(KeysTableColumns.CREATED_AT, now)
        return super.insert(uri, values)
    }

    override fun createUriMatcher(contentUri: Uri, @NonNls basePath: String): UriMatcher {
        val matcher = super.createUriMatcher(contentUri, basePath)
        matcher.addURI(contentUri.authority, "$basePath/by-uid/*", KEY_BY_UID)
        return matcher
    }

    private fun sanitize(value: String): String {
        val ret = StringBuilder()
        for (c in value) {
            if (c in '0'..'9' || c in 'a'..'z' || c in 'A'..'Z')
                ret.append(c)
        }
        return ret.toString()
    }

    override fun appendWheres(builder: SQLiteQueryBuilder, matcher: UriMatcher, uri: Uri) {
        when (matcher.match(uri)) {
            KEY_BY_UID -> builder.appendWhere(KeysTableColumns.CARD_ID + "= \""
                    + sanitize(uri.pathSegments[2]) + "\"")
            else -> super.appendWheres(builder, matcher, uri)
        }
    }

    companion object {
        @NonNls
        val AUTHORITY = BuildConfig.APPLICATION_ID + ".keyprovider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/keys")
        val CONTENT_BY_UID_URI: Uri = Uri.withAppendedPath(CONTENT_URI, "/by-uid")
        private const val KEY_BY_UID = 1000
    }
}
