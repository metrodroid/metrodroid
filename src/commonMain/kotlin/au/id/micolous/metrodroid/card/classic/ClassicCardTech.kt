/*
 * ClassicCardTech.kt
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.util.ImmutableByteArray

interface ClassicCardTech {
    fun authenticate(sectorIndex: Int, key: ClassicSectorKey): Boolean
    val sectorCount: Int
    val tagId: ImmutableByteArray
    fun readBlock(block: Int): ImmutableByteArray
    fun getBlockCountInSector(sectorIndex: Int): Int
    fun sectorToBlock(sectorIndex: Int): Int
    val subType: ClassicCard.SubType
        get() = ClassicCard.SubType.CLASSIC
}
