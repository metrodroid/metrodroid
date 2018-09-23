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
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.Pair;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;

public class ClassicStaticKeys extends CardKeys {
    private static final String TAG = "ClassicStaticKeys";
    private static final String SECTOR_IDX = "sector";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VALUE = "key";
    private static final String KEY_HASH = "hash";
    private static final String KEY_CARD = "card";
    private static final String KEYS = "keys";
    public static final String JSON_TAG_ID_DESC = "description";

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

    public static ClassicStaticKeys fromMCTStatic(Context context, InputStream keyData) throws IOException, JSONException {
        InputStream i = context.getAssets().open("static_key_map.json",
                AssetManager.ACCESS_STREAMING);
        byte []keyMap = IOUtils.toByteArray(i);
        i.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(keyData));
        ClassicStaticKeys st = ClassicStaticKeys.fromJSON(new JSONObject(new String(keyMap)));
        String line;
        while ((line=br.readLine()) != null) {
            String tl = line.trim();
            if (tl.startsWith("#"))
                continue;
            try {
                byte[] key = Utils.hexStringToByteArray(tl);
                st.qualify(key);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing line " + line, e);
            }
        }
        st.removeEmpty();
        if (st.mKeys.isEmpty())
            return null;
        return st;
    }

    private void removeEmpty() {
        Map<String, Pair<Integer, Integer>> stats = new HashMap<>();
        for (Map.Entry<Integer, List<ClassicSectorKeyWrapper>> cl : mKeys.entrySet()) {
            List<ClassicSectorKeyWrapper> cln = new ArrayList<>();
            for (ClassicSectorKeyWrapper cle : cl.getValue()) {
                Pair<Integer, Integer> stat;
                if (stats.containsKey(cle.getCard()))
                    stat = stats.get(cle.getCard());
                else
                    stat = Pair.create(0, 0);
                if (cle.getKey() != null) {
                    stat = Pair.create(stat.first + 1, stat.second);
                    cln.add(cle);
                } else {
                    Log.d(TAG, "Missing " + cle.mHash);
                    stat = Pair.create(stat.first, stat.second + 1);
                }
                stats.put(cle.getCard(), stat);
            }
            if (cln.size() == 0)
                mKeys.remove(cl.getKey());
            else
                mKeys.put(cl.getKey(), cln);
        }
        StringBuilder desc = new StringBuilder();
        for (Map.Entry<String, Pair<Integer, Integer>> stat : stats.entrySet()) {
            Log.d(TAG, "card " + stat.getKey() + " " + stat.getValue().first + "/" + stat.getValue().second + " (have/missing)");
            String curdesc = null;
            if (stat.getValue().second == 0 && stat.getValue().first != 0)
                curdesc = stat.getKey();
            if (stat.getValue().second != 0 && stat.getValue().first != 0)
                curdesc = Utils.localizeString(R.string.partial_keys, stat.getKey());
            if (curdesc != null && desc.length() != 0)
                desc.append(", ");
            if (curdesc != null)
                desc.append(curdesc);
        }
        mDescription = desc.toString();
    }

    private void qualify(byte[] key) {
        for (Map.Entry<Integer, List<ClassicSectorKeyWrapper>> cl : mKeys.entrySet()) {
            for (ClassicSectorKeyWrapper cle : cl.getValue())
                cle.qualify(key);
        }
    }

    public String getDescription() {
        return mDescription;
    }

    public static class ClassicSectorKeyWrapper extends ClassicSectorKey {
        private String mHash;
        private String mCard;

        private ClassicSectorKeyWrapper(JSONObject json) throws JSONException {
            super(json.getString(KEY_TYPE), json.has(KEY_VALUE) ?
                    Utils.hexStringToByteArray(json.getString(KEY_VALUE)) : null);
            mHash = json.optString(KEY_HASH);
            mCard = json.optString(KEY_CARD);
        }

        private void qualify(byte[] key) {
            if (getKey() != null || mCard == null || mHash == null)
                return;
            if (Utils.checkKeyHash(key, mCard, mHash) < 0)
                return;
            mKey = key;
        }

        private JSONObject toJSON(int idx) throws JSONException {
            JSONObject json = new JSONObject();
            json.put(SECTOR_IDX, idx);
            json.put(KEY_TYPE, getType());
            if (getKey() != null)
                json.put(KEY_VALUE, Utils.getHexString(getKey()));
            if (mHash != null)
                json.put(KEY_HASH, mHash);
            if (mCard != null)
                json.put(KEY_CARD, mCard);
            return json;
        }

        public String getCard() {
            return mCard;
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONArray keysJson = new JSONArray();
        for (Map.Entry<Integer, List<ClassicSectorKeyWrapper>> sector : mKeys.entrySet())
            for (ClassicSectorKeyWrapper key : sector.getValue())
                keysJson.put(key.toJSON(sector.getKey()));

        JSONObject json = new JSONObject();
        json.put(KEYS, keysJson);
        if (mDescription != null)
            json.put(JSON_TAG_ID_DESC, mDescription);
        return json;
    }

    public static ClassicStaticKeys fromJSON(JSONObject jsonRoot) throws JSONException {
        ClassicStaticKeys ins = new ClassicStaticKeys();
        JSONArray keysJSON = jsonRoot.getJSONArray(KEYS);
        for (int i = 0; i < keysJSON.length(); i++) {
            JSONObject json = keysJSON.getJSONObject(i);

            int idx = json.getInt(SECTOR_IDX);
            if (!ins.mKeys.containsKey(idx))
                ins.mKeys.put(idx, new ArrayList<>());

            ins.mKeys.get(idx).add(
                    new ClassicSectorKeyWrapper(json));
        }
        return ins;
    }

    public List<ClassicSectorKey> getCandidates(int sectorIndex) {
        if (!mKeys.containsKey(sectorIndex))
            return Collections.emptyList();
        return (List<ClassicSectorKey>)(List<?>)mKeys.get(sectorIndex);
    }
}
