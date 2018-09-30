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
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.provider.CardKeyProvider;
import au.id.micolous.metrodroid.provider.KeysTableColumns;
import au.id.micolous.metrodroid.util.KeyFormat;
import au.id.micolous.metrodroid.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import au.id.micolous.metrodroid.MetrodroidApplication;

public abstract class CardKeys {
    public static final String JSON_KEY_TYPE_KEY = "KeyType";
    public static final String TYPE_MFC = "MifareClassic";
    public static final String TYPE_MFC_STATIC = "MifareClassicStatic";
    public static final String JSON_TAG_ID_KEY = "TagId";

    public static final String CLASSIC_STATIC_TAG_ID = "staticclassic";

    /**
     * Retrieves a MIFARE Classic card keys from storage by its UID.
     * @param tagId The UID to look up (4 bytes)
     * @return Matching {@link ClassicCardKeys}, or null if not found
     */
    @Nullable
    public static ClassicCardKeys forTagId(byte[] tagId) throws JSONException {
        String tagIdString = Utils.getHexString(tagId);
        return fromUri(Uri.withAppendedPath(CardKeyProvider.CONTENT_BY_UID_URI, tagIdString));
    }

    /**
     * Retrieves all statically defined MIFARE Classic keys.
     * @return All {@link ClassicCardKeys}, or null if not found
     */
    @Nullable
    public static ClassicCardKeys forStaticClassic() throws JSONException {
        return fromUri(Uri.withAppendedPath(CardKeyProvider.CONTENT_BY_UID_URI, CLASSIC_STATIC_TAG_ID));
    }

    /**
     * Retrieves a key by its internal ID.
     * @return Matching {@link ClassicCardKeys}, or null if not found.
     */
    public static ClassicCardKeys forID(int id) throws JSONException {
        return fromUri(Uri.withAppendedPath(CardKeyProvider.CONTENT_URI, Integer.toString(id)));
    }

    @Nullable
    public static ClassicCardKeys fromUri(Uri uri) throws JSONException {
        MetrodroidApplication app = MetrodroidApplication.getInstance();
        Cursor cursor = app.getContentResolver().query(uri,
                null, null, null, null);
        if (cursor == null) {
            return null;
        }

        if (cursor.moveToFirst()) {
            return CardKeys.fromCursor(cursor);
        } else {
            return null;
        }
    }

    private static ClassicCardKeys fromCursor(Cursor cursor) throws JSONException {
        String cardType = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE));
        String keyData = cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA));

        JSONObject keyJSON = new JSONObject(keyData);

        // We only return a single set of keys when given a card ID
        if (cardType.equals(TYPE_MFC)) {
            return ClassicCardKeys.fromJSON(keyJSON, KeyFormat.JSON_MFC);
        }

        // Static key requests should give all of the static keys.
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

    public abstract String getType();
}
