/*
 * ClassicSectorKeys.java
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


import au.id.micolous.metrodroid.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class ClassicSectorKey {
    public static final String TYPE_KEYA = "KeyA";
    public static final String TYPE_KEYB = "KeyB";
    private static final String TYPE = "type";
    private static final String KEY = "key";
    private String mType;
    protected byte[] mKey;

    public ClassicSectorKey(String type, byte[] key) {
        mType = type;
        mKey = key;
    }

    public static ClassicSectorKey fromJSON(JSONObject json) throws JSONException {
        return new ClassicSectorKey(json.getString(TYPE), Utils.hexStringToByteArray(json.getString(KEY)));
    }

    public String getType() {
        return mType;
    }

    public byte[] getKey() {
        return mKey;
    }

    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            json.put(TYPE, mType);
            if (mKey != null)
                json.put(KEY, Utils.getHexString(mKey));
            return json;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
