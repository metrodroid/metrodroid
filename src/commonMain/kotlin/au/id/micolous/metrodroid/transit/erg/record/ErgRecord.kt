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

package au.id.micolous.metrodroid.transit.erg.record

import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Represents a record inside of an ERG MIFARE Classic based card.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */
open class ErgRecord protected constructor() {

    override fun toString(): String = "[ErgRecord]"

    companion object {
        private const val TAG = "ErgRecord"
        private const val DEBUG = false

        /**
         * Deserialises a given record from a card.  This will select the appropriate record type based
         * on the block number on the card.
         * @param input A single block of input from a MIFARE Classic card (16 bytes)
         * @param sectorIndex The 0-indexed sector number.
         * @param blockIndex The 0-indexed block number within a sector.
         * @return An ErgRecord containing a deserialisation of the data, or null if not known.
         */
        fun recordFromBytes(input: ImmutableByteArray, sectorIndex: Int, blockIndex: Int): ErgRecord? {
            var record: ErgRecord? = null

            if (sectorIndex == 0) {
                if (blockIndex == 1) {
                    record = ErgPreambleRecord.recordFromBytes(input)
                } else if (blockIndex == 2) {
                    record = ErgMetadataRecord.recordFromBytes(input)
                }
            } else if (sectorIndex == 3) {
                // Sometimes block 0 + 1, sometimes 1 + 2, sometimes 0 + 2...
                record = ErgBalanceRecord.recordFromBytes(input)
            } else if (sectorIndex == 4 && blockIndex == 2 || sectorIndex in 5..11) {
                // NB: Sector 8 is generally empty, but grab that record anyway.
                record = ErgPurseRecord.recordFromBytes(input)
            }

            if (DEBUG) {
                if (record != null) {
                    Log.d(TAG, "Sector $sectorIndex, Block $blockIndex: $record")
                }

                Log.d(TAG, input.toHexString())
            }

            return record
        }
    }
}
