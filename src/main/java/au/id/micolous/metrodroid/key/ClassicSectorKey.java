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


import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;

public class ClassicSectorKey implements Comparable<ClassicSectorKey>, Cloneable {
    private static final String TYPE_KEYA = "KeyA";
    private static final String TYPE_KEYB = "KeyB";

    private static final String KEY_TYPE = "type";
    private static final String KEY_VALUE = "key";
    static final int KEY_LEN = 6;

    private KeyType mType = KeyType.UNKNOWN;
    private byte[] mKey = null;

    public enum KeyType {
        UNKNOWN,
        A,
        B,
        MULTIPLE;

        @StringRes
        public int getFormatRes() {
            switch (this) {
                case A:
                    return R.string.classic_key_format_a;
                case B:
                    return R.string.classic_key_format_b;
                default:
                    return R.string.classic_key_format;
            }
        }

        public KeyType inverse() {
            if (this == B) {
                return A;
            } else {
                return B;
            }
        }

        public String toString() {
            if (this == B) {
                return TYPE_KEYB;
            } else {
                return TYPE_KEYA;
            }
        }

        public static KeyType fromString(String keyType) {
            if (keyType.equals(TYPE_KEYB)) {
                return B;
            } else {
                return A;
            }
        }

        public static final class Transform implements org.simpleframework.xml.transform.Transform<KeyType> {
            @Override
            public KeyType read(String value) {
                return fromString(value);
            }

            @Override
            public String write(KeyType value) {
                return value.toString();
            }
        }
    }

    protected ClassicSectorKey() {}

    public static ClassicSectorKey wellKnown(@NonNull byte[] b) {
        ClassicSectorKey k = fromDump(b);
        k.setType(KeyType.A);
        return k;
    }

    public static ClassicSectorKey fromDump(@NonNull byte[] b) {
        if (b.length != KEY_LEN) {
            throw new IllegalArgumentException(
                    String.format(Locale.ENGLISH, "Key data must be %d bytes, got %d", KEY_LEN, b.length));
        }

        ClassicSectorKey k = new ClassicSectorKey();
        k.mKey = b;
        return k;
    }

    public static ClassicSectorKey fromDump(@NonNull byte[] b, int offset) {
        byte[] data = Arrays.copyOfRange(b, offset, offset + ClassicSectorKey.KEY_LEN);
        return fromDump(data);
    }

    public static ClassicSectorKey fromJSON(JSONObject json) throws JSONException {
        ClassicSectorKey k = new ClassicSectorKey();
        fromJSON(k, json);
        return k;
    }

    protected static void fromJSON(ClassicSectorKey k, JSONObject json) throws JSONException {
        KeyType kt = KeyType.UNKNOWN;

        if (json.has(KEY_TYPE) && !json.isNull(KEY_TYPE) && !json.getString(KEY_TYPE).isEmpty()) {
            String t = json.getString(KEY_TYPE);
            if (TYPE_KEYA.equals(t)) {
                kt = KeyType.A;
            } else if (TYPE_KEYB.equals(t)) {
                kt = KeyType.B;
            }
        }

        byte[] keyData = Utils.hexStringToByteArray(json.getString(KEY_VALUE));

        // Check that the key is the correct length
        if (keyData.length != KEY_LEN) {
            throw new JSONException(
                    String.format(Locale.ENGLISH, "Expected %d bytes in key, got %d",
                            KEY_LEN, keyData.length
                    ));
        }

        // Checks completed, pass the data back.
        k.setType(kt);
        k.mKey = keyData;
    }

    public void setType(KeyType k) {
        mType = k;
    }

    public KeyType getType() {
        return mType;
    }

    public byte[] getKey() {
        return mKey;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        switch (mType) {
            case A:
                json.put(KEY_TYPE, TYPE_KEYA);
                break;

            case B:
                json.put(KEY_TYPE, TYPE_KEYB);
                break;
        }

        if (mKey != null) {
            json.put(KEY_VALUE, Utils.getHexString(mKey));
        }

        return json;
    }

    @Override
    public int compareTo(@NonNull ClassicSectorKey o) {
        int d = mType.compareTo(o.mType);
        if (d != 0) return d;

        if (Arrays.equals(mKey, o.mKey)) {
            return 0;
        } else {
            return 1;
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ClassicSectorKey clone() {
        ClassicSectorKey k = new ClassicSectorKey();
        k.mKey = ArrayUtils.clone(mKey);
        k.mType = mType;
        return k;
    }

    public void invertType() {
        mType = mType.inverse();
    }
}
