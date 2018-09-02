/*
 * NextfareUtil.java
 *
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
package au.id.micolous.metrodroid.transit.nextfare;

import android.util.Log;

import au.id.micolous.metrodroid.util.Utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Misc utilities for parsing Nextfare Cards
 *
 * @author Michael Farrell
 */
public final class NextfareUtil {

    /**
     * Date format:
     * <p>
     * Top two bytes:
     * 0001111 1100 00100 = 2015-12-04
     * yyyyyyy mmmm ddddd
     * <p>
     * Bottom 11 bits = minutes since 00:00
     * Time is represented in localtime
     * <p>
     * Assumes that data has not been byte-reversed.
     *
     * @param input Bytes of input representing the timestamp to parse
     * @param offset Offset in byte to timestamp
     * @return Date and time represented by that value
     */
    public static GregorianCalendar unpackDate(byte[] input, int offset, TimeZone timeZone) {
        int timestamp = Utils.byteArrayToIntReversed(input, offset, 4);
        int minute = Utils.getBitsFromInteger(timestamp, 16, 11);
        int year = Utils.getBitsFromInteger(timestamp, 9, 7) + 2000;
        int month = Utils.getBitsFromInteger(timestamp, 5, 4);
        int day = Utils.getBitsFromInteger(timestamp, 0, 5);

        Log.i("nextfareutil", "unpackDate: " + minute + " minutes, " + year + '-' + month + '-' + day);

        if (minute > 1440)
            throw new AssertionError(String.format(Locale.ENGLISH, "Minute > 1440 (%d)", minute));
        if (minute < 0)
            throw new AssertionError(String.format(Locale.ENGLISH, "Minute < 0 (%d)", minute));

        if (day > 31) throw new AssertionError(String.format(Locale.ENGLISH, "Day > 31 (%d)", day));
        if (month > 12)
            throw new AssertionError(String.format(Locale.ENGLISH, "Month > 12 (%d)", month));

        GregorianCalendar d = new GregorianCalendar(timeZone);
        d.set(Calendar.YEAR, year);
        d.set(Calendar.MONTH, month - 1);
        d.set(Calendar.DAY_OF_MONTH, day);

        // Needs to be set explicitly, as this defaults to localtime.
        d.set(Calendar.HOUR_OF_DAY, 0);
        d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);

        d.add(Calendar.MINUTE, minute);

        return d;
    }

    private NextfareUtil() {
    }
}
