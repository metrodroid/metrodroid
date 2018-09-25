/*
 * CardKeys.java
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

package au.id.micolous.metrodroid.key;

import android.database.Cursor;
import android.net.Uri;

import au.id.micolous.metrodroid.provider.CardKeyProvider;
import au.id.micolous.metrodroid.provider.KeysTableColumns;
import au.id.micolous.metrodroid.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import au.id.micolous.metrodroid.MetrodroidApplication;

public abstract class CardKeys {
    public static final String JSON_KEY_TYPE_KEY = "KeyType";
    public static final String TYPE_MFC = "MifareClassic";
    public static final String TYPE_MFC_STATIC = "MifareClassicStatic";
    public static final String JSON_TAG_ID_KEY = "TagId";
    public static final String CLASSIC_STATIC_TAG_ID = "staticclassic";

    public static CardKeys forTagId(byte[] tagId) throws Exception {
        String tagIdString = Utils.getHexString(tagId);
        MetrodroidApplication app = MetrodroidApplication.getInstance();
        Cursor cursor = app.getContentResolver().query(Uri.withAppendedPath(CardKeyProvider.CONTENT_URI, tagIdString), null, null, null, null);
        if (cursor.moveToFirst()) {
            return CardKeys.fromCursor(cursor);
        } else {
            return null;
        }
    }

    public static CardKeys forStaticClassic() throws JSONException {
        MetrodroidApplication app = MetrodroidApplication.getInstance();
        Cursor cursor = app.getContentResolver().query(Uri.withAppendedPath(CardKeyProvider.CONTENT_URI, CLASSIC_STATIC_TAG_ID), null, null, null, null);
        if (cursor.moveToFirst()) {
            return CardKeys.fromCursor(cursor);
        } else {
            return null;
        }
    }

    private static CardKeys fromCursor(Cursor cursor) throws JSONException {
        String cardType = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE));
        String keyData = cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA));

        JSONObject keyJSON = new JSONObject(keyData);

        if (cardType.equals(TYPE_MFC)) {
            return ClassicCardKeys.fromJSON(keyJSON);
        }

        if (cardType.equals(TYPE_MFC_STATIC)) {
            ClassicStaticKeys keys = ClassicStaticKeys.fromJSON(keyJSON);
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE)).equals(TYPE_MFC_STATIC))
                    try {
                        keys.mergeJSON(new JSONObject(cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA))));
                    } catch (JSONException ignored) {
                    }
            }
            return keys;
        }

        throw new IllegalArgumentException("Unknown card type for key: " + cardType);
    }

    public abstract JSONObject toJSON() throws JSONException;
}
