/*
 * ClassicSector.kt
 *
 * Copyright 2012-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

abstract class ClassicSector {
    abstract val blocks: List<ClassicBlock>
    abstract fun getRawData(idx: Int): ListItem
    abstract val raw: ClassicSectorRaw

    val key: ClassicSectorKey?
        get() {
            val keyA = raw.keyA
            if (keyA != null)
                return ClassicSectorKey(key = keyA, type = ClassicSectorKey.KeyType.A, bundle = "read-back")
            val keyB = raw.keyB
            if (keyB != null)
                return ClassicSectorKey(key = keyB, type = ClassicSectorKey.KeyType.B, bundle = "read-back")
            return null
        }

    fun readBlocks(startBlock: Int, blockCount: Int): ImmutableByteArray {
        var data = ImmutableByteArray.empty()
        for (index in startBlock until startBlock + blockCount) {
            val blockData = getBlock(index).data
            data = data.plus(blockData)
        }
        return data
    }

    fun getBlock(index: Int) = blocks[index]
    operator fun get(index: Int): ClassicBlock = getBlock(index)

    companion object {
        fun create(raw: ClassicSectorRaw): ClassicSector {
            if (raw.isUnauthorized)
                return UnauthorizedClassicSector(raw)
            if (raw.error != null)
                return InvalidClassicSector(raw)
            return ClassicSectorValid(raw)
        }
    }
}

