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

import android.os.Parcelable
import android.util.SparseArray
import android.util.SparseIntArray

import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.android.parcel.Parcelize

@Parcelize
class ErgIndexRecord private constructor(
        val version : Int,
        val version2 : Int,
        private val allocations : SparseIntArray
) : Parcelable {
    fun readRecord(sectorNum: Int, blockNum: Int, data: ImmutableByteArray): ErgRecord? {
        val block = sectorNum * 3 + blockNum
        val type = allocations.get(block, 0)
        val f = FACTORIES.get(type) ?: return null
        return f.recordFromBytes(data)
    }

    override fun toString(): String {
        return "[${javaClass.simpleName}: version=$version/$version2, allocations=$allocations]"
    }

    companion object {
        private val FACTORIES = SparseArray<ErgRecord.Factory>()

        init {
            FACTORIES.put(0x03, ErgBalanceRecord.FACTORY)
            // TODO: implement alternate record 1 (0x04) and 2 (0x05)
            FACTORIES.put(0x14, ErgPurseRecord.FACTORY)
            FACTORIES.put(0x15, ErgPurseRecord.FACTORY)
            FACTORIES.put(0x16, ErgPurseRecord.FACTORY)
            FACTORIES.put(0x17, ErgPurseRecord.FACTORY)
            FACTORIES.put(0x18, ErgPurseRecord.FACTORY)
            FACTORIES.put(0x19, ErgPurseRecord.FACTORY)
            FACTORIES.put(0x1a, ErgPurseRecord.FACTORY)
            FACTORIES.put(0x1b, ErgPurseRecord.FACTORY)
            FACTORIES.put(0x1c, ErgPurseRecord.FACTORY)
            FACTORIES.put(0x1d, ErgPurseRecord.FACTORY)
        }

        fun recordFromSector(sector: ClassicSector): ErgIndexRecord {
            return recordFromBytes(
                    sector.getBlock(0).data,
                    sector.getBlock(1).data,
                    sector.getBlock(2).data)
        }

        fun recordFromBytes(block0: ImmutableByteArray, block1: ImmutableByteArray, block2: ImmutableByteArray): ErgIndexRecord {
            val version = block0.byteArrayToInt(1, 2)
            val allocations = SparseIntArray()

            var o = 6
            for (x in 3..15) {
                allocations.put(o + x, block0.byteArrayToInt(x, 1))
            }

            o += 16
            repeat(16) {
                allocations.put(o + it, block1.byteArrayToInt(it, 1))
            }

            o += 16
            repeat(10) {
                allocations.put(o + it, block2.byteArrayToInt(it, 1))
            }

            val version2 = block2.byteArrayToInt(11, 2)
            return ErgIndexRecord(version, version2, allocations)
        }
    }
}
