/*
 * ClassicKeys.java
 *
 * Copyright 2012-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Helper for access to MIFARE Classic keys.
 *
 * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys for details about
 * the data formats implemented here.
 */
public class ClassicCardKeys extends CardKeys {
    static final String KEYS = "keys";
    private static final int KEY_LEN = 6;

    private final ClassicSectorKey[] mSectorKeys;

    ClassicCardKeys() {
        mSectorKeys = null;
    }

    private ClassicCardKeys(ClassicSectorKey[] sectorKeys) {
        mSectorKeys = sectorKeys;
    }

    /**
     * Reads ClassicCardKeys in "raw" (farebotkeys) format.
     *
     * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys#raw-farebotkeys
     */
    public static ClassicCardKeys fromDump(String keyType, byte[] keyData) {
        List<ClassicSectorKey> keys = new ArrayList<>();

        int numSectors = keyData.length / KEY_LEN;
        for (int i = 0; i < numSectors; i++) {
            int start = i * KEY_LEN;
            keys.add(new ClassicSectorKey(keyType, Arrays.copyOfRange(keyData, start, start + KEY_LEN)));
        }

        return new ClassicCardKeys(keys.toArray(new ClassicSectorKey[keys.size()]));
    }

    /**
     * Reads ClassicCardKeys from the internal (JSON) format.
     *
     * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys#json
     */
    public static ClassicCardKeys fromJSON(JSONObject json) throws JSONException {
        JSONArray keysJson = json.getJSONArray(KEYS);
        ClassicSectorKey[] sectorKeys = new ClassicSectorKey[keysJson.length()];
        for (int i = 0; i < keysJson.length(); i++) {
            sectorKeys[i] = ClassicSectorKey.fromJSON(keysJson.getJSONObject(i));
        }
        return new ClassicCardKeys(sectorKeys);
    }

    /**
     * Gets the keys for a particular sector on the card.
     *
     * Must be overridden by subclasses.
     * @param sectorNumber The sector number to retrieve the key for
     * @return All candidate {@link ClassicSectorKey} for that sector, or an empty list if there is
     *         no known key, or the sector is out of range.
     */
    @NonNull
    public List<ClassicSectorKey> getCandidates(int sectorNumber) {
        if (mSectorKeys == null || sectorNumber >= mSectorKeys.length) {
            return Collections.emptyList();
        }

        return Collections.singletonList(mSectorKeys[sectorNumber]);
    }

    /**
     * Gets all keys for the card.
     *
     * Must be overridden by subclasses.
     * @return All {@link ClassicSectorKey} for the card.
     */
    @NonNull
    public List<ClassicSectorKey> keys() {
        if (mSectorKeys == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(mSectorKeys);
    }

    /**
     * Serialises this class to JSON format.
     *
     * Must be overridden by subclasses.
     * @return A JSON blob with all the sectors associated with this card.
     */
    public JSONObject toJSON() throws JSONException {
        if (mSectorKeys == null) {
            return new JSONObject();
        }

        JSONArray keysJson = new JSONArray();
        for (ClassicSectorKey key : mSectorKeys) {
            keysJson.put(key.toJSON());
        }

        JSONObject json = new JSONObject();
        json.put(KEYS, keysJson);
        return json;
    }
}
