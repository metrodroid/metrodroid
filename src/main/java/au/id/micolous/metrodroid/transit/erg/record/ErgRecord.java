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

/**
 * Represents a record inside of an ERG MIFARE Classic based card.
 */
public class ErgRecord {
    private static final String TAG = "ErgRecord";

    protected ErgRecord() {
    }

    public static ErgRecord recordFromBytes(byte[] input, int sectorIndex, int blockIndex) {
        ErgRecord record = null;

        if (sectorIndex == 0) {
            if (blockIndex == 1) {
                record = ErgPreambleRecord.recordFromBytes(input);
            } else if (blockIndex == 2) {
                record = ErgMetadataRecord.recordFromBytes(input);
            }
        } else if (sectorIndex == 3) {
            if (blockIndex == 0 || blockIndex == 2) {
                record = ErgBalanceRecord.recordFromBytes(input);
            }
        } else if (sectorIndex >= 5 && sectorIndex <= 9) {
            record = ErgPurseRecord.recordFromBytes(input);
        }

        Log.d(TAG, String.format(Locale.ENGLISH, "Sector %d, Block %d: %s", sectorIndex, blockIndex,
                record == null ? "null" : record.getClass().getSimpleName()));
        return record;
    }

}
