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

import java.util.TimeZone;

import au.id.micolous.metrodroid.util.Utils;

/**
 * Represents a record on a Nextfare card
 * This fans out parsing to subclasses.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
public class NextfareRecord {
    private static final String TAG = "NextfareRecord";

    protected NextfareRecord() {
    }

    public static NextfareRecord recordFromBytes(byte[] input, int sectorIndex, int blockIndex, TimeZone timeZone) {
        NextfareRecord record = null;
        //noinspection StringConcatenation
        Log.d(TAG, "Record: " + Utils.getHexString(input));

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

}
