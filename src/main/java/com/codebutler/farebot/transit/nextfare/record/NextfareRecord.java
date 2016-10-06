/*
 * NextfareRecord.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.nextfare.record;

import android.util.Log;

import com.codebutler.farebot.util.Utils;

/**
 * Represents a record on a Nextfare card
 */
public class NextfareRecord {
    private static final String TAG = "NextfareRecord";

    protected NextfareRecord() {}

    public static NextfareRecord recordFromBytes(byte[] input, int sectorIndex, int blockIndex) {
        NextfareRecord record = null;
        Log.d(TAG, "Record: " + Utils.getHexString(input));

        if (sectorIndex == 1 && blockIndex <= 1) {
            Log.d(TAG, "Balance record");
            record = NextfareBalanceRecord.recordFromBytes(input);
        } else if (sectorIndex == 2) {
            Log.d(TAG, "Top-up record");
            record = NextfareTopupRecord.recordFromBytes(input);
        } else if (sectorIndex == 3) {
            Log.d(TAG, "Travel pass record");
            record = NextfareTravelPassRecord.recordFromBytes(input);
        } else if (sectorIndex >= 5 && sectorIndex <= 8) {
            Log.d(TAG, "Tap on/off record");
            record = NextfareTapRecord.recordFromBytes(input);
        } else {
            // attempt autodetection


            switch (input[0]) {
                case 0x01:
                    // Check if the next byte is not null
                    if (input[1] == 0x00) {
                        // Metadata record, which we don't understand yet
                        Log.d(TAG, "Metadata record 01 / 00 unknown");
                        break;
                    } else if (input[1] == 0x01) {
                        if (input[13] == 0x00) {
                            // Some other metadata type
                            Log.d(TAG, "Metadata record 01 / 01 / 00@13 unknown");
                            return null;
                        }

                        Log.d(TAG, "Top-up record 01 / 01");
                        record = NextfareTopupRecord.recordFromBytes(input);
                    } else if (input[1] > 0x03) {
                        // 0x03 = TAP card "value = 256" probably not a balance, doesn't appear in any history
                        // 0x23 = go card
                        // 0x30 = regular balance on TAP, Go
                        // 0x3[1-f] = possibly for student go card?

                        Log.d(TAG, "Balance record 01 / " + Utils.getHexString(new byte[]{input[1]}));
                        record = NextfareBalanceRecord.recordFromBytes(input);
                    } else {
                        Log.d(TAG, "Unknown 01");
                    }
                    break;

                case 0x31:
                    if (input[1] == 0x01) {
                        if (input[12] == 0x00) {
                            // Some other metadata type
                            Log.d(TAG, "Metadata record 31 / 01 / 00@12 unknown");
                            return null;
                        }
                        Log.d(TAG, "Top-up record 31 / 01");
                        record = NextfareTopupRecord.recordFromBytes(input);
                    } else {
                        Log.d(TAG, "Tap record 31");
                        record = NextfareTapRecord.recordFromBytes(input);
                    }
                    break;

                default:
                    // Unknown record type
                    Log.d(TAG, "totally unknown record type");
                    break;
            }
        }
        return record;
    }

}
