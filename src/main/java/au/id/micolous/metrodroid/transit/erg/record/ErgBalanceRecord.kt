/*
 * ErgBalanceRecord.kt
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

import au.id.micolous.metrodroid.xml.ImmutableByteArray

/**
 * Represents a balance record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#balance-records
 */
class ErgBalanceRecord private constructor(
    /**
     * The balance of the card, in cents.
     *
     * @return int number of cents.
     */
    val balance: Int,
    val version: Int,
    private val mAgency: Int) : ErgRecord(), Comparable<ErgBalanceRecord> {

    override fun compareTo(other: ErgBalanceRecord): Int {
        // So sorting works, we reverse the order so highest number is first.
        return other.version.compareTo(this.version)
    }

    override fun toString() = "[ErgBalanceRecord: agencyID=$mAgency, balance=$balance, version=$version]"

    companion object {
        val FACTORY: ErgRecord.Factory = object : ErgRecord.Factory() {
            override fun recordFromBytes(block: ImmutableByteArray): ErgRecord? {
                //if (input[0] != 0x01) throw new AssertionError();

                return if (block[7].toInt() != 0x00 || block[8].toInt() != 0x00) {
                    // There is another record type that gets mixed in here, which has these
                    // bytes set to non-zero values. In that case, it is not the balance record.
                    null
                } else ErgBalanceRecord(
                        block.byteArrayToInt(11, 4),
                        block.byteArrayToInt(1, 2),
                        // Present on MFF, not CHC Metrocard
                        block.byteArrayToInt(5, 2))
            }
        }
    }
}
