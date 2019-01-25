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
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.ClassicSectorRaw
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.serializers.CardImporter
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.io.InputStream
import kotlinx.io.charsets.Charsets
import kotlinx.io.core.String

// TODO: remove it when kotlinx will get this
private fun InputStream.forEachLine(function: (String) -> Unit) {
    // Largest MFC is 4K. hex brings it up to 8K. Newlines and +Sector
    // Add less than 2x. So 16K chars is the most we are interested in
    // IT should be ASCII, but let's be safe and allocate 32K
    val buf = ByteArray(32768) { 0 }
    val actualLen = this.read(buf, 0, buf.size)
    val str = String(bytes = buf.sliceArray(0 until actualLen), charset = Charsets.UTF_8)
    str.split('\n', '\r').filter { it.isNotEmpty() }.forEach(function)
}

/**
 * Class to read files built by MIFARE Classic Tool.
 */
class MctCardImporter : CardImporter {
    override fun readCard(stream: InputStream): Card? {
        val sectors = mutableListOf<ClassicSector>()
        var curSector = -1
        var maxSector = -1
        var curBlocks = mutableListOf<ImmutableByteArray>()
        var lastBlock: String? = null
        stream.forEachLine { lineRaw ->
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
                    curBlocks.add(ImmutableByteArray.fromHex(line.replace('-', '0')))
                }
            }
        }

        flushSector(sectors, curSector, curBlocks, lastBlock)
        val uid: ImmutableByteArray
        if (sectors[0] !is UnauthorizedClassicSector) {
            val block0 = sectors[0].getBlock(0).data
            if (block0[0].toInt() == 4)
                uid = block0.copyOfRange(0, 7)
            else
                uid = block0.copyOfRange(0, 4)
        } else
            uid = ImmutableByteArray.fromASCII("fake")

        if (maxSector <= 15)
            maxSector = 15 // 1K
        else if (maxSector <= 31)
            maxSector = 31 // 2K
        else if (maxSector <= 39)
            maxSector = 39 // 4K

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
        val raw = ClassicSectorRaw(curBlocks, keyA, keyB, false, null)
        sectors.add(ClassicSector.create(raw))
    }
}
