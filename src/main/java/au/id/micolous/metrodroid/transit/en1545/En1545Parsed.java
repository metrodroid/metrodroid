/*
 * En1545Fixed.java
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

package au.id.micolous.metrodroid.transit.en1545;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.id.micolous.metrodroid.ui.ListItem;

public class En1545Parsed implements Parcelable {
    private final Map<Pair<String, String>, Object> mMap;

    public En1545Parsed() {
        mMap = new HashMap<>();
    }

    public En1545Parsed(Parcel in) {
        mMap = in.readHashMap(En1545Parsed.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(mMap);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<En1545Parsed> CREATOR = new Creator<En1545Parsed>() {
        @Override
        public En1545Parsed createFromParcel(Parcel in) {
            return new En1545Parsed(in);
        }

        @Override
        public En1545Parsed[] newArray(int size) {
            return new En1545Parsed[size];
        }
    };

    public void insertInt (String name, String path, int value) {
        mMap.put(Pair.create(name, path), value);
    }

    public void insertString (String name, String path, String value) {
        mMap.put(Pair.create(name, path), value);
    }

    private static String makeFullName(Pair<String, String> k) {
        return k.first + k.second;
    }

    public List<ListItem> getInfo(Set<String> skipSet) {
        ArrayList<ListItem> li = new ArrayList<>();
        for (Map.Entry<Pair<String, String>, Object> kv: mMap.entrySet()) {
            if (skipSet.contains(kv.getKey().first))
                continue;
            Object l = kv.getValue();
            String fullName = makeFullName(kv.getKey());
            if (l instanceof Integer)
                li.add(new ListItem(fullName, "0x" + Integer.toHexString((Integer) l)));
            if (l instanceof String)
                li.add(new ListItem(fullName, (String) l));
        }
        return li;
    }

    public List<ListItem> getInfo() {
        return getInfo(Collections.EMPTY_SET);
    }

    public String getString(String separator, Set<String> skipSet) {
        StringBuilder ret = new StringBuilder();
        for (Map.Entry<Pair<String, String>, Object> kv: mMap.entrySet()) {
            if (skipSet.contains(kv.getKey().first))
                continue;
            ret.append(makeFullName(kv.getKey())).append(" = ");
            Object l = kv.getValue();
            if (l instanceof Integer)
                ret.append("0x").append(Integer.toHexString((Integer) l));
            if (l instanceof String)
                ret.append("\"").append((String) l).append("\"");
            ret.append(separator);
        }
        return ret.toString();
    }



    @Override
    public String toString() {
        return "[" + getString(", ", Collections.EMPTY_SET) + "]";
    }

    public Integer getInt(String name, String path) {
        if (!mMap.containsKey(Pair.create(name, path)))
            return null;
        return (Integer) mMap.get(Pair.create(name, path));
    }

    public Integer getInt(String name, int... ipath) {
        StringBuilder path = new StringBuilder();
        for (int iel : ipath)
            path.append("/").append(Integer.toString(iel));
        if (!mMap.containsKey(Pair.create(name, path.toString())))
            return null;
        return (Integer) mMap.get(Pair.create(name, path.toString()));
    }

    public int getIntOrZero(String name, String path) {
        if (!mMap.containsKey(Pair.create(name, path)))
            return 0;
        return (Integer) mMap.get(Pair.create(name, path));
    }

    public boolean contains(String name, String path) {
        return mMap.containsKey(Pair.create(name, path));
    }

    public int getIntOrZero(String name) {
        return getIntOrZero(name, "");
    }

    public Integer getInt(String name) {
        return getInt(name, "");
    }

    public boolean contains(String name) {
        return contains(name, "");
    }
}
