package au.id.micolous.metrodroid.test

import android.util.Log
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicReader
import au.id.micolous.metrodroid.card.classic.InvalidClassicSector
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.key.CardKeysEmbed
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import au.id.micolous.metrodroid.xml.toImmutable
import junit.framework.TestCase
import org.junit.Test

class ClassicReaderTest : BaseInstrumentedTest() {
    fun doTest(path: String) {
        val auth = CardKeysEmbed(baseDir = "$path/keys")
        for (dump in listAsset("$path/dumps").orEmpty()) {
            val raw = loadSmallAssetBytes("$path/dumps/$dump").toImmutable()
            val card = VirtualClassic(raw)
            val read = ClassicReader.readCard(context, auth, card, object : TagReaderFeedbackInterface {
                override fun updateStatusText(msg: String?) = Unit
                override fun updateProgressBar(progress: Int, max: Int) = Unit
                override fun showCardType(cardInfo: CardInfo?) = Unit
            })
            val addMsg = "dump = $path/dumps/$dump"
            Log.d(TAG, "$addMsg: read, starting verification")
            verifyRead(read, raw, setOf(), addMsg)
            val blockCount = raw.size / 16
            val maxReads = blockCount
            val maxAuths = 4 * card.sectorCount + 8

            val actualReads = card.readCounter
            val actualAuths = card.authCounter
            Log.d(TAG, "$addMsg reads/auths/sectors = $actualReads/$actualAuths/${card.sectorCount}")
            TestCase.assertTrue("$addMsg: Made $actualReads reads which is more than $maxReads",
                    actualReads <= maxReads)
            TestCase.assertTrue("$addMsg: Made $actualAuths auths which is more than $maxAuths",
                    actualAuths <= maxAuths)
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
        TestCase.assertEquals("$addMsg: Wrong number of sectors", sectorCount, read.sectors.size)
        for ((idx, sec) in read.sectors.withIndex()) {
            TestCase.assertEquals("$addMsg: Sectors are not in order", idx, sec.index)
            val blockCount = if (idx >= 32) 16 else 4

            if (idx in unauthorizedSet) {
                TestCase.assertTrue("$addMsg: Sector $idx should be unauthorized but isn't", sec is UnauthorizedClassicSector)
            } else {
                TestCase.assertTrue("$addMsg: Sector $idx should be ok but isn't",
                        sec !is UnauthorizedClassicSector && sec !is InvalidClassicSector)
            }

            for (block in 0 until blockCount - 1) {
                TestCase.assertFalse ("$addMsg: Sector $idx, block $block should be ok but isn't",
                        read[idx, block].isUnauthorized)
                val actual = read[idx, block].data!!
                val expected = raw.copyOfRange(blockOffset * 16,
                        (blockOffset + 1) * 16)
                TestCase.assertTrue (
                        "$addMsg: blocks don't match: $actual expected: $expected",
                        actual.contentEquals(expected))

                blockOffset++
            }

            val key = sec.key!!

            TestCase.assertTrue("$addMsg: KeyType must be A or B",
                    key.type == ClassicSectorKey.KeyType.A
                            || key.type == ClassicSectorKey.KeyType.B)

            val keyOffset = if (key.type == ClassicSectorKey.KeyType.B) 10 else 0
            val keyExpected = raw.copyOfRange(blockOffset * 16 + keyOffset,
                    blockOffset * 16 + keyOffset + 6)
            TestCase.assertTrue(
                    "$addMsg: keys don't match: ${key.key} expected: $keyExpected",
                    key.key.contentEquals(keyExpected))

            val accBitsExpected = raw.copyOfRange(blockOffset * 16 + 6,
                    blockOffset * 16 + 10)
            val accBitsActual = read[idx, blockCount - 1].data.copyOfRange(6, 10)
            TestCase.assertTrue(
                    "$addMsg: access bits don't match: $accBitsActual expected: $accBitsExpected",
                    accBitsActual.contentEquals(accBitsExpected))
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