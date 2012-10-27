/*
 * ClassicKeys.java
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

package com.codebutler.farebot.keys;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ClassicCardKeys extends CardKeys {
    private static final String KEYS = "keys";

    private ClassicSectorKey[] mSectorKeys;

    public static ClassicCardKeys fromJSON(JSONObject json) throws JSONException {
        ClassicSectorKey[] sectorKeys = new ClassicSectorKey[json.length()];
        JSONArray keysJson = json.getJSONArray(KEYS);
        for (int i = 0; i < keysJson.length(); i++) {
            sectorKeys[i] = ClassicSectorKey.fromJSON(keysJson.getJSONObject(i));
        }
        return new ClassicCardKeys(sectorKeys);
    }

    private ClassicCardKeys(ClassicSectorKey[] sectorKeys) {
        mSectorKeys = sectorKeys;
    }

    public ClassicSectorKey keyForSector(int sectorNumber) {
        return mSectorKeys[sectorNumber];
    }

    public JSONObject toJSON() throws JSONException {
        JSONArray keysJson = new JSONArray();
        for (ClassicSectorKey key : mSectorKeys) {
            keysJson.put(key.toJSON());
        }

        JSONObject json = new JSONObject();
        json.put(KEYS, keysJson);
        return json;
    }
}