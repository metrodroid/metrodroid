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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

public class En1545Parsed implements Parcelable {
    private final Map<String, Object> mMap;

    public En1545Parsed() {
        mMap = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
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
        mMap.put(makeFullName(name, path), value);
    }

    public void insertString (String name, String path, String value) {
        mMap.put(makeFullName(name, path), value);
    }

    private static String makeFullName(String name, String path) {
        if (path == null || path.equals("") || path.equals(""))
            return name;
        return path + "/" + name;
    }

    public List<ListItem> getInfo(Set<String> skipSet) {
        ArrayList<ListItem> li = new ArrayList<>();
        for (Map.Entry<String, Object> kv: mMap.entrySet()) {
            if (skipSet.contains(getBaseName(kv.getKey())))
                continue;
            Object l = kv.getValue();
            String fullName = kv.getKey();
            if (l instanceof Integer)
                li.add(new ListItem(fullName, "0x" + Integer.toHexString((Integer) l)));
            if (l instanceof String)
                li.add(new ListItem(fullName, (String) l));
        }
        return li;
    }

    private String getBaseName(String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }

    public String makeString(String separator, Set<String> skipSet) {
        StringBuilder ret = new StringBuilder();
        for (Map.Entry<String, Object> kv: mMap.entrySet()) {
            if (skipSet.contains(getBaseName(kv.getKey())))
                continue;
            ret.append(kv.getKey()).append(" = ");
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
        return "[" + makeString(", ", Collections.EMPTY_SET) + "]";
    }

    @Nullable
    public Integer getInt(String name, String path) {
        if (!mMap.containsKey(makeFullName(name, path)))
            return null;
        return (Integer) mMap.get(makeFullName(name, path));
    }

    @Nullable
    public Integer getInt(String name, int... ipath) {
        StringBuilder path = new StringBuilder();
        for (int iel : ipath)
            path.append("/").append(Integer.toString(iel));
        if (!mMap.containsKey(makeFullName(name, path.toString())))
            return null;
        return (Integer) mMap.get(makeFullName(name, path.toString()));
    }

    public int getIntOrZero(String name, String path) {
        Integer i = getInt(name, path);
        return i == null ? 0 : i;
    }

    @Nullable
    public String getString(String name, String path) {
        if (!mMap.containsKey(makeFullName(name, path)))
            return null;
        return (String) mMap.get(makeFullName(name, path));
    }

    private Pair<Calendar,Integer> getTimeStampFlags(@NonNull String name, TimeZone tz) {
        if (contains(name + "DateTime"))
            return Pair.create(En1545FixedInteger.parseTimeSec(getIntOrZero(name + "DateTime"), tz), 3);
        if (contains(name + "Time") && contains(name + "Date"))
            return Pair.create(En1545FixedInteger.parseTime(getIntOrZero(name + "Date"), getIntOrZero(name + "Time"), tz),
                    3);
        if (contains(name + "TimeLocal") && contains(name + "Date"))
            return Pair.create(En1545FixedInteger.parseTimeLocal(getIntOrZero(name + "Date"), getIntOrZero(name + "TimeLocal"), tz),
                    3);
        if (contains(name + "Date"))
            return Pair.create(En1545FixedInteger.parseDate(getIntOrZero(name + "Date"), tz), 2);
        if (contains(name + "TimeLocal"))
            return Pair.create(En1545FixedInteger.parseTimeLocal(0, getIntOrZero(name + "TimeLocal"), tz),
                    1);
        return null;
    }

    @Nullable
    public Calendar getTimeStamp(@NonNull String name, TimeZone tz) {
        Pair<Calendar,Integer> timeFlag = getTimeStampFlags(name, tz);
        if (timeFlag == null)
            return null;
        return timeFlag.first;
    }

    boolean getTimeStampContainsTime(@NonNull String name) {
        if (contains(name + "DateTime"))
            return true;
        if (contains(name + "Time") && contains(name + "Date"))
            return true;
        if (contains(name + "TimeLocal") && contains(name + "Date"))
            return true;
        if (contains(name + "Date"))
            return false;
        if (contains(name + "TimeLocal"))
            return true;
        return false;
    }

    @Nullable
    public String getTimeStampString(String name, TimeZone tz) {
        Pair<Calendar,Integer> timeFlag = getTimeStampFlags(name, tz);
        if (timeFlag == null)
            return null;
        Calendar cal = timeFlag.first;
        cal = TripObfuscator.maybeObfuscateTS(cal);
        switch (timeFlag.second) {
            case 3:
                return Utils.dateTimeFormat(cal).toString();
            case 2:
                return Utils.dateFormat(cal).toString();
            case 1:
                return Utils.timeFormat(cal).toString();
        }
        return null;
    }

    public boolean contains(String name, String path) {
        return mMap.containsKey(makeFullName(name, path));
    }

    public int getIntOrZero(String name) {
        return getIntOrZero(name, "");
    }

    @Nullable
    public Integer getInt(String name) {
        return getInt(name, "");
    }

    @Nullable
    public String getString(String name) {
        return getString(name, "");
    }

    public boolean contains(String name) {
        return contains(name, "");
    }

    public En1545Parsed append(byte[] data, int off, En1545Field field) {
        field.parseField(data, off, "", this);
        return this;
    }

    public En1545Parsed append(byte[] data, En1545Field field) {
        field.parseField(data, 0, "", this);
        return this;
    }
}
