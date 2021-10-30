/*
 * MctCardImporter.kt
 *
 * Copyright 2018-2019 Google
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
package au.id.micolous.metrodroid.serializers.classic

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.ClassicSectorRaw
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.serializers.CardImporter
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.lines

/**
 * Class to read files built by MIFARE Classic Tool.
 */
class MctCardImporter : CardImporter {
    override fun readCard(stream: Input): Card? {
        val sectors = mutableListOf<ClassicSector>()
        var curSector = -1
        var maxSector = -1
        var curBlocks = mutableListOf<ImmutableByteArray>()
        var lastBlock: String? = null
        stream.lines().forEach { lineRaw ->
            val line = lineRaw.trim()
            if (line.startsWith("+Sector:")) {
                flushSector(sectors, curSector, curBlocks, lastBlock)
                curBlocks = mutableListOf()
                curSector = line.substring(8).trim { it <= ' ' }.toInt()
                if (curSector > maxSector)
                    maxSector = curSector
            } else {
                if (curSector >= 0) {
                    lastBlock = line
                    curBlocks.add(
                            if (line == "--------------------------------")
                                ImmutableByteArray.empty()
                            else
                                ImmutableByteArray.fromHex(line.replace('-', '0')))
                }
            }
        }

        flushSector(sectors, curSector, curBlocks, lastBlock)
        val uid = if (sectors[0] !is UnauthorizedClassicSector) {
            val block0 = sectors[0].getBlock(0).data
            MfcCardImporter.block0ToUid(block0)
        } else
            ImmutableByteArray.fromASCII("fake")

        when {
            maxSector <= 15 -> maxSector = 15 // 1K
            maxSector <= 31 -> maxSector = 31 // 2K
            maxSector <= 39 -> maxSector = 39 // 4K
        } // 4K

        while (sectors.size <= maxSector)
            sectors.add(UnauthorizedClassicSector())

        return Card(tagId = uid, scannedAt = TimestampFull.now(),
                mifareClassic = ClassicCard(sectors))
    }


    private fun flushSector(sectors: MutableList<ClassicSector>, curSector: Int,
                            curBlocks: List<ImmutableByteArray>, lastBlock: String?) {
        if (curSector < 0)
            return
        while (sectors.size < curSector)
            sectors.add(UnauthorizedClassicSector())
        val keyA = if (lastBlock?.startsWith("-") == false) {
            ImmutableByteArray.fromHex(lastBlock.substring(0, 12))
        } else
            null
        val keyB = if (lastBlock?.endsWith("-") == false) {
            ImmutableByteArray.fromHex(lastBlock.substring(20, 32))
        } else
            null
        val raw = if (keyA == null && keyB == null)
            ClassicSectorRaw(curBlocks, keyA, keyB, true, "Unauthorized")
        else
            ClassicSectorRaw(curBlocks, keyA, keyB, false, null)
        sectors.add(ClassicSector.create(raw))
    }
}
