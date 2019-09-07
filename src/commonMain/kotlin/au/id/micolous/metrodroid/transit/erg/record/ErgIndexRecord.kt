/*
 * ErgIndexRecord.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class ErgIndexRecord private constructor(
        val version : Int,
        val version2 : Int,
        private val allocations : Map<Int, Int>
) : Parcelable {
    fun readRecord(sectorNum: Int, blockNum: Int, data: ImmutableByteArray): ErgRecord? {
        val block = sectorNum * 3 + blockNum
        val type = allocations[block] ?: 0
        val f = FACTORIES[type] ?: return null
        return f(data)
    }

    override fun toString(): String {
        return "[ErgIndexRecord: version=$version/$version2, allocations=$allocations]"
    }

    companion object {
        private val FACTORIES = mapOf(
            0x03 to ErgBalanceRecord.Companion::recordFromBytes,
            // TODO: implement alternate record 1 (0x04) and 2 (0x05)
            0x14 to ErgPurseRecord.Companion::recordFromBytes,
            0x15 to ErgPurseRecord.Companion::recordFromBytes,
            0x16 to ErgPurseRecord.Companion::recordFromBytes,
            0x17 to ErgPurseRecord.Companion::recordFromBytes,
            0x18 to ErgPurseRecord.Companion::recordFromBytes,
            0x19 to ErgPurseRecord.Companion::recordFromBytes,
            0x1a to ErgPurseRecord.Companion::recordFromBytes,
            0x1b to ErgPurseRecord.Companion::recordFromBytes,
            0x1c to ErgPurseRecord.Companion::recordFromBytes,
            0x1d to ErgPurseRecord.Companion::recordFromBytes)

        fun recordFromSector(sector: ClassicSector): ErgIndexRecord {
            return recordFromBytes(
                    sector.getBlock(0).data,
                    sector.getBlock(1).data,
                    sector.getBlock(2).data)
        }

        fun recordFromBytes(block0: ImmutableByteArray, block1: ImmutableByteArray, block2: ImmutableByteArray): ErgIndexRecord {
            val version = block0.byteArrayToInt(1, 2)
            val allocations = mutableMapOf<Int, Int>()

            var o = 6
            for (x in 3..15) {
                allocations[o + x] = block0.byteArrayToInt(x, 1)
            }

            o += 16
            repeat(16) {
                allocations[o + it] = block1.byteArrayToInt(it, 1)
            }

            o += 16
            repeat(10) {
                allocations[o + it] = block2.byteArrayToInt(it, 1)
            }

            val version2 = block2.byteArrayToInt(11, 2)
            return ErgIndexRecord(version, version2, allocations)
        }
    }
}
