/*
 * ErgRecord.java
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

package au.id.micolous.metrodroid.transit.erg.record;

import android.util.Log;

import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.metrodroid.xml.ImmutableByteArray;

/**
 * Represents a record inside of an ERG MIFARE Classic based card.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */
public class ErgRecord {
    private static final String TAG = "ErgRecord";

    // Flipping this to true shows more data from the records in Logcat.
    private static final boolean DEBUG = true;

    protected ErgRecord() {
    }

    /**
     * Deserialises a given record from a card.  This will select the appropriate record type based
     * on the block number on the card.
     * @param input A single block of input from a MIFARE Classic card (16 bytes)
     * @param sectorIndex The 0-indexed sector number.
     * @param blockIndex The 0-indexed block number within a sector.
     * @return An ErgRecord containing a deserialisation of the data, or null if not known.
     */
    public static ErgRecord recordFromBytes(ImmutableByteArray input, int sectorIndex, int blockIndex, TimeZone timeZone) {
        ErgRecord record = null;

        if (sectorIndex == 0) {
            if (blockIndex == 1) {
                record = ErgPreambleRecord.recordFromBytes(input);
            } else if (blockIndex == 2) {
                record = ErgMetadataRecord.recordFromBytes(input);
            }
        } else if (sectorIndex == 3) {
            // Sometimes block 0 + 1, sometimes 1 + 2, sometimes 0 + 2...
            record = ErgBalanceRecord.recordFromBytes(input);
        } else if ((sectorIndex == 4 && blockIndex == 2)
                || (sectorIndex >= 5 && sectorIndex <= 11)) {
            // NB: Sector 8 is generally empty, but grab that record anyway.
            record = ErgPurseRecord.recordFromBytes(input);
        }

        if (record != null) {
            Log.d(TAG, String.format(Locale.ENGLISH, "Sector %d, Block %d: %s",
                    sectorIndex, blockIndex,
                    DEBUG ? record.toString() : record.getClass().getSimpleName()));
            if (DEBUG) {
                Log.d(TAG, input.getHexString());
            }
        }

        return record;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "[%s]",
                getClass().getSimpleName());
    }
}
