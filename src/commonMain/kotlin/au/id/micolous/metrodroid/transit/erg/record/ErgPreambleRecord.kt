/*
 * ErgPreambleRecord.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
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

import au.id.micolous.metrodroid.transit.erg.ErgTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Represents a preamble record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#preamble-record
 */
class ErgPreambleRecord private constructor(val cardSerial: String?) : ErgRecord {
    /**
     * Returns the card serial number. Returns null on old cards.
     */
    override fun toString() = "[ErgPreambleRecord: serial=$cardSerial]"

    companion object {
        private val OLD_CARD_ID = byteArrayOf(0x00, 0x00, 0x00)

        fun recordFromBytes(input: ImmutableByteArray): ErgPreambleRecord {

            // Check that the record is valid for a preamble
            if (!input.copyOfRange(0, ErgTransitData.SIGNATURE.size).contentEquals(ErgTransitData.SIGNATURE)) {
                throw IllegalArgumentException("Preamble signature does not match")
            }

            // This is not set on 2012-era cards
            return ErgPreambleRecord(
                    cardSerial =
                    if (input.copyOfRange(10, 13).contentEquals(OLD_CARD_ID)) {
                        null
                    } else {
                        input.getHexString(10, 4)
                    })
        }
    }
}
