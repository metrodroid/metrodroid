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

/**
 * Represents a record on a SEQ Go Card (Translink).
 */
public class NextfareRecord {
    protected NextfareRecord() {}

    public static NextfareRecord recordFromBytes(byte[] input) {
        NextfareRecord record = null;
        switch (input[0]) {
            case 0x01:
                // Check if the next byte is not null
                if (input[1] == 0x00) {
                    // Metadata record, which we don't understand yet
                    break;
                } else if (input[1] == 0x01) {
                    if (input[13] == 0x00) {
                        // Some other metadata type
                        return null;
                    }
                    record = NextfareTopupRecord.recordFromBytes(input);
                } else {
                    record = NextfareBalanceRecord.recordFromBytes(input);
                }
                break;

            case 0x31:
                if (input[1] == 0x01) {
                    if (input[13] == 0x00) {
                        // Some other metadata type
                        return null;
                    }
                    record = NextfareTopupRecord.recordFromBytes(input);
                } else {
                    record = NextfareTapRecord.recordFromBytes(input);
                }
                break;

            default:
                // Unknown record type
                break;
        }

        return record;
    }

}
