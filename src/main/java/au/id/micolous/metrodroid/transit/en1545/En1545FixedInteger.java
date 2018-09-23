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

import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import au.id.micolous.metrodroid.util.Utils;

public class En1545FixedInteger implements En1545Field {
    private final int mLen;
    private final String mName;

    public En1545FixedInteger(String name, int len) {
        mName = name;
        mLen = len;
    }

    @Override
    public int parseField(byte[] b, int off, String path, En1545Parsed holder) {
        try {
            holder.insertInt(mName, path, Utils.getBitsFromBuffer(b, off, mLen));
        } catch (Exception e) {
        }
        return off + mLen;
    }

    private static final long UTC_EPOCH;

    private static long getEpoch(TimeZone tz) {
        GregorianCalendar epoch = new GregorianCalendar(tz);
        epoch.set(Calendar.YEAR,1997);
        epoch.set(Calendar.MONTH,Calendar.JANUARY);
        epoch.set(Calendar.DAY_OF_MONTH,1);
        epoch.set(Calendar.HOUR_OF_DAY,0);
        epoch.set(Calendar.MINUTE,0);
        epoch.set(Calendar.SECOND,0);
        epoch.set(Calendar.MILLISECOND,0);
        return epoch.getTimeInMillis();
    }

    static {
        UTC_EPOCH = getEpoch(TimeZone.getTimeZone("UTC"));
    }

    @Nullable
    private static Calendar parseTime(long epoch, int d, int t, TimeZone tz) {
        if (d == 0 && t == 0)
            return null;
        GregorianCalendar g = new GregorianCalendar(tz);
        g.setTimeInMillis(epoch);
        g.add(Calendar.DAY_OF_YEAR, d);
        g.add(Calendar.MINUTE, t);
        return g;
    }

    @Nullable
    public static Calendar parseTime(int d, int t, TimeZone tz) {
        return parseTime(UTC_EPOCH, d, t, tz);
    }

    @Nullable
    public static Calendar parseTimeLocal(int d, int t, TimeZone tz) {
        return parseTime(getEpoch(tz), d, t, tz);
    }

    @Nullable
    public static Calendar parseDate(int d, TimeZone tz) {
        return parseTime(getEpoch(tz), d, 0, tz);
    }

    @Nullable
    public static Calendar parseTimeSec(int val, TimeZone tz) {
        if (val == 0)
            return null;
        GregorianCalendar g = new GregorianCalendar(tz);
        g.setTimeInMillis(UTC_EPOCH);
        g.add(Calendar.SECOND, val);
        return g;
    }

    public static Calendar parseBCDDate(int date, TimeZone tz) {
        GregorianCalendar g = new GregorianCalendar(tz);
        g.set(Utils.convertBCDtoInteger(date >> 16),
                Utils.convertBCDtoInteger(((date >> 8) & 0xff)) - 1,
                Utils.convertBCDtoInteger(date & 0xff),
                0, 0, 0);
        return g;
    }

    public static En1545FixedInteger date(String name) {
        return new En1545FixedInteger(name + "Date", 14);
    }

    public static En1545FixedInteger time(String name) {
        return new En1545FixedInteger(name + "Time", 11);
    }

    public static En1545FixedInteger BCDdate(String name) {
        return new En1545FixedInteger(name, 32);
    }

    public static En1545Field dateTime(String name) {
        return new En1545FixedInteger(name + "DateTime", 30);
    }

    public static En1545Field timeLocal(String name) {
        return new En1545FixedInteger(name + "TimeLocal", 11);
    }
}
