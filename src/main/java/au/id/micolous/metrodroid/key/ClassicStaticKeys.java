/*
 * ClassicStaticKeys.java
 *
 * Copyright 2018 Google
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

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.id.micolous.metrodroid.util.Utils;

/**
 * Helper for access to static MIFARE Classic keys. This can be used for keys that should be
 * attempted on multiple cards.
 *
 * This is only really useful when a transit agency doesn't implement key diversification.
 *
 * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys#static-json for
 * a file format description.
 */
public class ClassicStaticKeys extends ClassicCardKeys {
    private static final String SECTOR_IDX = "sector";
    public static final String JSON_TAG_ID_DESC = "Description";

    // Sparse Arrays are annoying to iterate through
    @SuppressLint("UseSparseArrays")
    private Map<Integer, List<ClassicSectorKeyWrapper>> mKeys = new HashMap<>();
    private String mDescription;

    public void mergeJSON(JSONObject string) throws JSONException {
        merge(fromJSON(string));
    }

    private void merge(ClassicStaticKeys classicStaticKeys) {
        for (Map.Entry<Integer, List<ClassicSectorKeyWrapper>> cl : classicStaticKeys.mKeys.entrySet()) {
            if (!mKeys.containsKey(cl.getKey()))
                mKeys.put(cl.getKey(), new ArrayList<>());
            mKeys.get(cl.getKey()).addAll(cl.getValue());
        }
    }

    public String getDescription() {
        return mDescription;
    }

    private static class ClassicSectorKeyWrapper extends ClassicSectorKey {
        private int mSectorIndex;

        private ClassicSectorKeyWrapper() {}

        public static ClassicSectorKeyWrapper fromJSON(JSONObject json) throws JSONException {
            ClassicSectorKeyWrapper w = new ClassicSectorKeyWrapper();
            w.mSectorIndex = json.getInt(SECTOR_IDX);
            fromJSON(w, json);
            return w;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject o = super.toJSON();
            o.put(SECTOR_IDX, mSectorIndex);
            return o;
        }

        int getSectorIndex() {
            return mSectorIndex;
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONArray keysJson = new JSONArray();
        for (Map.Entry<Integer, List<ClassicSectorKeyWrapper>> sector : mKeys.entrySet()) {
            for (ClassicSectorKey key : sector.getValue()) {
                keysJson.put(key.toJSON());
            }
        }

        JSONObject json = new JSONObject();
        json.put(KEYS, keysJson);
        if (mDescription != null)
            json.put(JSON_TAG_ID_DESC, mDescription);
        return json;
    }

    public static ClassicStaticKeys fromJSON(JSONObject jsonRoot) throws JSONException {
        ClassicStaticKeys ins = new ClassicStaticKeys();
        ins.mDescription = jsonRoot.optString(JSON_TAG_ID_DESC);
        JSONArray keysJSON = jsonRoot.getJSONArray(KEYS);
        for (int i = 0; i < keysJSON.length(); i++) {
            JSONObject json = keysJSON.getJSONObject(i);

            ClassicSectorKeyWrapper w = ClassicSectorKeyWrapper.fromJSON(json);

            if (!ins.mKeys.containsKey(w.getSectorIndex())) {
                ins.mKeys.put(w.getSectorIndex(), new ArrayList<>());
            }

            ins.mKeys.get(w.getSectorIndex()).add(w);
        }

        ins.setLengthAndReturn(jsonRoot);
        return ins;
    }

    @Override
    @NonNull
    public List<ClassicSectorKey> getCandidates(int sectorIndex) {
        if (!mKeys.containsKey(sectorIndex))
            return Collections.emptyList();
        return (List<ClassicSectorKey>)(List<?>)mKeys.get(sectorIndex);
    }

    @NonNull
    @Override
    public List<ClassicSectorKey> keys() {
        List<ClassicSectorKey> allKeys = new ArrayList<>();
        for (List<ClassicSectorKeyWrapper> keys : mKeys.values()) {
            allKeys.addAll(keys);
        }
        return allKeys;
    }

    @Override
    public String getType() {
        return CardKeys.TYPE_MFC_STATIC;
    }

    @Nullable
    @Override
    public String getUID() {
        return CLASSIC_STATIC_TAG_ID;
    }
}
