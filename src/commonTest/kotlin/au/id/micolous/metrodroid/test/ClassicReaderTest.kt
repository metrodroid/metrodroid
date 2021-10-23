/*
 * ClassicReaderTest.kt
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

package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicReader
import au.id.micolous.metrodroid.card.classic.InvalidClassicSector
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.key.CardKeysFileReader
import au.id.micolous.metrodroid.key.CardKeysFromFiles
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassicReaderTest : BaseInstrumentedTest() {
    private fun keyReader(path: String): CardKeysFileReader = object : CardKeysFileReader {
        override fun listFiles(dir: String): List<String>? = listAsset("$path/keys/$dir")

        override fun readFile(fileName: String): String?
            = loadSmallAssetBytesSafe("$path/keys/$fileName")?.decodeToString()
    }

     /*   context, baseDir = "$path/keys"
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }*/

    fun doTest(path: String) {
        val auth = CardKeysFromFiles(keyReader(path))
        for (dump in listAsset("$path/dumps").orEmpty()) {
            val raw = loadSmallAssetBytes("$path/dumps/$dump").toImmutable()
            val card = VirtualClassic(raw)
            val read = ClassicReader.readCard(auth, card, MockFeedbackInterface.get())
            val addMsg = "dump = $path/dumps/$dump"
            Log.d(TAG, "$addMsg: read, starting verification")
            verifyRead(read, raw, setOf(), addMsg)
            val blockCount = raw.size / 16
            val maxAuths = 4 * card.sectorCount + 8

            val actualReads = card.readCounter
            val actualAuths = card.authCounter
            Log.d(TAG, "$addMsg reads/auths/sectors = $actualReads/$actualAuths/${card.sectorCount}")
            assertTrue(
                message = "$addMsg: Made $actualReads reads which is more than $blockCount",
                actual = actualReads <= blockCount
            )
            assertTrue(
                    message = "$addMsg: Made $actualAuths auths which is more than $maxAuths",
                    actual = actualAuths <= maxAuths)
        }
    }

    private fun verifyRead(read: ClassicCard, raw: ImmutableByteArray, unauthorizedSet: Set<Int>, addMsg: String) {
        val sectorCount = when (raw.size) {
            1024 -> 16
            2048 -> 32
            4096 -> 40
            else -> 0
        }
        var blockOffset = 0
        assertEquals(sectorCount, read.sectors.size, "$addMsg: Wrong number of sectors")
        for ((idx, sec) in read.sectors.withIndex()) {
            val blockCount = if (idx >= 32) 16 else 4

            if (idx in unauthorizedSet) {
                assertTrue(sec is UnauthorizedClassicSector, "$addMsg: Sector $idx should be unauthorized but isn't")
            } else {
                assertTrue(actual = sec !is UnauthorizedClassicSector && sec !is InvalidClassicSector,
                        message = "$addMsg: Sector $idx should be ok but isn't")
            }

            for (block in 0 until blockCount - 1) {
                assertFalse (read[idx, block].isUnauthorized, "$addMsg: Sector $idx, block $block should be ok but isn't")
                val actual = read[idx, block].data
                val expected = raw.copyOfRange(blockOffset * 16,
                        (blockOffset + 1) * 16)
                assertTrue (
                        message = "$addMsg: blocks don't match: $actual expected: $expected",
                        actual = actual.contentEquals(expected))

                blockOffset++
            }

            val key = (sec.keyA ?: sec.keyB)!!

            assertTrue(message = "$addMsg: KeyType must be A or B",
                    actual = key.type == ClassicSectorKey.KeyType.A
                            || key.type == ClassicSectorKey.KeyType.B)

            val keyOffset = if (key.type == ClassicSectorKey.KeyType.B) 10 else 0
            val keyExpected = raw.copyOfRange(blockOffset * 16 + keyOffset,
                    blockOffset * 16 + keyOffset + 6)
            assertTrue(
                    message = "$addMsg: keys don't match: ${key.key} expected: $keyExpected",
                    actual = key.key.contentEquals(keyExpected))

            val accBitsExpected = raw.copyOfRange(blockOffset * 16 + 6,
                    blockOffset * 16 + 10)
            val accBitsActual = read[idx, blockCount - 1].data.copyOfRange(6, 10)
            assertTrue(
                    message = "$addMsg: access bits don't match: $accBitsActual expected: $accBitsExpected",
                    actual = accBitsActual.contentEquals(accBitsExpected))
            blockOffset++
        }
    }

    // Synthetic dumps are not prepared yet, so dummy-out the tests for now
    @Test
    fun testDummy() {
    }

    companion object {
        private const val TAG = "ClassicReaderTest"
    }
}
