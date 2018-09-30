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
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.util.KeyFormat;

/**
 * Helper for access to MIFARE Classic keys.
 *
 * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys for details about
 * the data formats implemented here.
 */
public class ClassicCardKeys extends CardKeys {
    static final String KEYS = "keys";

    @Nullable
    private String mUID = null;
    private final ClassicSectorKey[] mSectorKeys;
    private int mSourceDataLength = 0;

    ClassicCardKeys() {
        mSectorKeys = null;
    }

    private ClassicCardKeys(@Nullable String uid, ClassicSectorKey[] sectorKeys) {
        mUID = uid;
        mSectorKeys = sectorKeys;
    }

    /**
     * Reads ClassicCardKeys in "raw" (farebotkeys) format.
     *
     * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys#raw-farebotkeys
     */
    public static ClassicCardKeys fromDump(byte[] keyData) {
        return fromDump(keyData, ClassicSectorKey.KeyType.UNKNOWN);
    }

    @VisibleForTesting
    public static ClassicCardKeys fromDump(byte[] keyData, ClassicSectorKey.KeyType keyType) {
        List<ClassicSectorKey> keys = new ArrayList<>();

        int numSectors = keyData.length / ClassicSectorKey.KEY_LEN;
        for (int i = 0; i < numSectors; i++) {
            int start = i * ClassicSectorKey.KEY_LEN;
            ClassicSectorKey k = ClassicSectorKey.fromDump(keyData, start);
            k.setType(keyType);
            keys.add(k);
        }

        ClassicCardKeys kk = new ClassicCardKeys(null, keys.toArray(new ClassicSectorKey[keys.size()]));
        kk.mSourceDataLength = keyData.length;
        return kk;
    }

    /**
     * Reads ClassicCardKeys from any JSON format.
     */
    public static ClassicCardKeys fromJSON(JSONObject json, KeyFormat format) throws JSONException, IllegalArgumentException {
        switch (format) {
            case JSON_MFC:
            case JSON_MFC_NO_UID:
                return internalFromJSON(json, format);

            case JSON_MFC_STATIC:
                return ClassicStaticKeys.fromJSON(json);
        }

        throw new IllegalArgumentException("Unsupported key file type: " + format);
    }

    /**
     * Reads ClassicCardKeys from the internal (JSON) format.
     *
     * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys#json
     */
    private static ClassicCardKeys internalFromJSON(JSONObject json, KeyFormat format) throws JSONException {
        String uid = null;
        if (format == KeyFormat.JSON_MFC) {
            uid = json.getString(JSON_TAG_ID_KEY);
        }

        JSONArray keysJson = json.getJSONArray(KEYS);
        ClassicSectorKey[] sectorKeys = new ClassicSectorKey[keysJson.length()];
        for (int i = 0; i < keysJson.length(); i++) {
            sectorKeys[i] = ClassicSectorKey.fromJSON(keysJson.getJSONObject(i));
        }

        return new ClassicCardKeys(uid, sectorKeys).setLengthAndReturn(json);
    }

    protected ClassicCardKeys setLengthAndReturn(JSONObject o) {
        mSourceDataLength = o.toString().length();
        return this;
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

    @Nullable
    public String getUID() {
        return mUID;
    }

    public void setUID(@NonNull String value) {
        mUID = value;
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
        if (mUID != null) {
            json.put(JSON_TAG_ID_KEY, mUID);
        }
        return json;
    }

    public void setAllKeyTypes(ClassicSectorKey.KeyType kt) {
        // Invalid for subclasses.
        if (mSectorKeys == null) {
            return;
        }

        for (ClassicSectorKey k : mSectorKeys) {
            k.setType(kt);
        }
    }

    @Nullable
    public ClassicSectorKey.KeyType getKeyType() {
        ClassicSectorKey.KeyType kt = null;
        for (ClassicSectorKey k : keys()) {
            if (kt == null) {
                kt = k.getType();
            } else {
                if (kt != k.getType()) {
                    return ClassicSectorKey.KeyType.MULTIPLE;
                }
            }
        }
        return kt;

    }

    public int getSourceDataLength() {
        return mSourceDataLength;
    }

    @Override
    public String getType() {
        return CardKeys.TYPE_MFC;
    }
}
