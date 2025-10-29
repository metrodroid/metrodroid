/*
 * ClassicCard.kt
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

import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import au.id.micolous.metrodroid.card.wrapAndroidExceptions
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import java.io.IOException

class ClassicCardTechAndroid(private val tech: MifareClassic,
                             override val tagId: ImmutableByteArray) : ClassicCardTech {
    override fun sectorToBlock(sectorIndex: Int) = tech.sectorToBlock(sectorIndex)

    override fun getBlockCountInSector(sectorIndex: Int) = tech.getBlockCountInSector(sectorIndex)

    override fun authenticate(sectorIndex: Int, key: ClassicSectorKey): Boolean = wrapAndroidExceptions {
        if (key.key.size != 6)
            false
        else if (key.type === ClassicSectorKey.KeyType.A || key.type === ClassicSectorKey.KeyType.UNKNOWN) {
            tech.authenticateSectorWithKeyA(sectorIndex, key.key.dataCopy)
        } else
            tech.authenticateSectorWithKeyB(sectorIndex, key.key.dataCopy)
    }

    override val sectorCount = tech.sectorCount

    override fun readBlock(block: Int): ImmutableByteArray = wrapAndroidExceptions {
        try {
            tech.readBlock(block).toImmutable()
        } catch (e: TagLostException) {
            throw e
        } catch (e: IOException) {
            ImmutableByteArray.empty()
        }
    }
}
