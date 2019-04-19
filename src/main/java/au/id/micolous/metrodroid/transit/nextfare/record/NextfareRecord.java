/*
 * NextfareRecord.java
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

package au.id.micolous.metrodroid.transit.nextfare.record;

import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.metrodroid.util.NumberUtils;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

/**
 * Represents a record on a Nextfare card
 * This fans out parsing to subclasses.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
public class NextfareRecord {
    private static final String TAG = "NextfareRecord";

    protected NextfareRecord() {
    }

    public static NextfareRecord recordFromBytes(ImmutableByteArray input, int sectorIndex, int blockIndex, TimeZone timeZone) {
        NextfareRecord record = null;
        Log.d(TAG, "Record: " + input);

        if (sectorIndex == 1 && blockIndex <= 1) {
            Log.d(TAG, "Balance record");
            record = NextfareBalanceRecord.recordFromBytes(input);
        } else if (sectorIndex == 1 && blockIndex == 2) {
            Log.d(TAG, "Configuration record");
            record = NextfareConfigRecord.recordFromBytes(input, timeZone);
        } else if (sectorIndex == 2) {
            Log.d(TAG, "Top-up record");
            record = NextfareTopupRecord.recordFromBytes(input, timeZone);
        } else if (sectorIndex == 3) {
            Log.d(TAG, "Travel pass record");
            record = NextfareTravelPassRecord.recordFromBytes(input, timeZone);
        } else if (sectorIndex >= 5 && sectorIndex <= 8) {
            Log.d(TAG, "Transaction record");
            record = NextfareTransactionRecord.recordFromBytes(input, timeZone);
        }
        return record;
    }

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
    public static GregorianCalendar unpackDate(ImmutableByteArray input, int offset, TimeZone timeZone) {
        int timestamp = input.byteArrayToIntReversed(offset, 4);
        int minute = NumberUtils.INSTANCE.getBitsFromInteger(timestamp, 16, 11);
        int year = NumberUtils.INSTANCE.getBitsFromInteger(timestamp, 9, 7) + 2000;
        int month = NumberUtils.INSTANCE.getBitsFromInteger(timestamp, 5, 4);
        int day = NumberUtils.INSTANCE.getBitsFromInteger(timestamp, 0, 5);

        //noinspection StringConcatenation,MagicCharacter
        Log.i(TAG, "unpackDate: " + minute + " minutes, " + year + '-' + month + '-' + day);

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
}
