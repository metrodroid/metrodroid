/*
 * CardKeyProvider.java
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

package au.id.micolous.metrodroid.provider;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.GregorianCalendar;

import au.id.micolous.farebot.BuildConfig;

public class CardKeyProvider extends BetterContentProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".keyprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/keys");
    public static final Uri CONTENT_BY_UID_URI = Uri.withAppendedPath(CONTENT_URI, "/by-uid");
    protected static final int KEY_BY_UID = 1000;

    public CardKeyProvider() {
        super(
                KeysDBHelper.class,
                KeysDBHelper.KEY_DIR_TYPE,
                KeysDBHelper.KEY_ITEM_TYPE,
                KeysTableColumns.TABLE_NAME,
                CONTENT_URI
        );
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long now = GregorianCalendar.getInstance().getTimeInMillis();
        values.put(KeysTableColumns.CREATED_AT, now);
        return super.insert(uri, values);
    }

    @Override
    protected UriMatcher createUriMatcher(Uri contentUri, String basePath) {
        UriMatcher matcher = super.createUriMatcher(contentUri, basePath);
        matcher.addURI(contentUri.getAuthority(), basePath + "/by-uid/*", KEY_BY_UID);
        return matcher;
    }

    @Override
    protected void appendWheres(SQLiteQueryBuilder builder, UriMatcher matcher, Uri uri) {
        switch (matcher.match(uri)) {
            case KEY_BY_UID:
                // FIXME: Prevent sql injection.
                builder.appendWhere(KeysTableColumns.CARD_ID + "= \"" + uri.getPathSegments().get(1) + "\"");
                break;
            default:
                super.appendWheres(builder, matcher, uri);
        }
    }
}
