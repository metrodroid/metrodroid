/*
 * NextfareTest.java
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

import android.nfc.tech.MifareClassic

import java.util.Arrays
import java.util.Calendar
import java.util.TimeZone

import au.id.micolous.metrodroid.card.classic.ClassicBlock
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData
import au.id.micolous.metrodroid.transit.msp_goto.MspGotoTransitData
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareBalanceRecord
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareConfigRecord
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.xml.ImmutableByteArray

import au.id.micolous.metrodroid.util.Utils.UTC
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests relating to Cubic Nextfare reader.
 */
class NextfareTest {
    private var originalTz: TimeZone? = null

    @BeforeTest
    fun setUp() {
        originalTz = TimeZone.getDefault()
        TimeZone.setDefault(UTC)
    }

    @AfterTest
    fun tearDown() {
        TimeZone.setDefault(originalTz)
    }

    @Test
    fun testExpiryDate() {
        val r20250602 = ImmutableByteArray.fromHex("01030000c2320000010200000000bf0c")
        val r20240925 = ImmutableByteArray.fromHex("0103000039310000010200000000924a")
        val r20180815 = ImmutableByteArray.fromHex("010300010f25000004000000fb75c2f7")

        var r: NextfareConfigRecord

        r = NextfareConfigRecord.recordFromBytes(r20250602, UTC)
        assertEquals("2025-06-02 00:00", Utils.isoDateTimeFormat(r.expiry))

        r = NextfareConfigRecord.recordFromBytes(r20240925, UTC)
        assertEquals("2024-09-25 00:00", Utils.isoDateTimeFormat(r.expiry))

        r = NextfareConfigRecord.recordFromBytes(r20180815, UTC)
        assertEquals("2018-08-15 00:00", Utils.isoDateTimeFormat(r.expiry))
    }

    @Test
    fun testTransactionRecord() {
        val rnull = ImmutableByteArray.fromHex("01000000000000000000000000007f28")

        val r: NextfareTransactionRecord?
        r = NextfareTransactionRecord.recordFromBytes(rnull, UTC)
        assertNull(r)
    }

    @Test
    fun testBalanceRecord() {
        // This tests the offset negative flag in seq_go.
        // NOTE: These records are synthetic and incomplete, but representative for the tests.
        // Checksums are wrong.

        // SEQ: $12.34, sequence 0x12
        val r1 = NextfareBalanceRecord.recordFromBytes(ImmutableByteArray.fromHex(
                "0128d20400000000000000000012ffff"))
        assertEquals(0x12, r1.version)
        assertEquals(1234, r1.balance)

        // SEQ: -$10.00, sequence 0x23
        val r2 = NextfareBalanceRecord.recordFromBytes(ImmutableByteArray.fromHex(
                "01a8e80300000000000000000023ffff"))
        assertEquals(0x23, r2.version)
        assertEquals(-1000, r2.balance)

        // SEQ: -$10.00, sequence 0x34
        val r3 = NextfareBalanceRecord.recordFromBytes(ImmutableByteArray.fromHex(
                "01a0e80300000000000000000034ffff"))
        assertEquals(0x34, r3.version)
        assertEquals(-1000, r3.balance)
    }

    private fun buildNextfareCard(uid: ImmutableByteArray,
                                  system_code: ImmutableByteArray,
                                  block2: ImmutableByteArray?): ClassicCard {
        assertEquals(6, system_code.size)
        assertEquals(4, uid.size)

        val trailer = ClassicBlock(3, ClassicBlock.TYPE_TRAILER,
                ImmutableByteArray.fromHex("ffffffffffff78778800a1a2a3a4a5a6"))

        val sectors = arrayOfNulls<ClassicSector>(16)

        for (sector_num in sectors.indices) {
            val blocks = arrayOfNulls<ClassicBlock>(4)
            if (sector_num == 0) {
                val b0 = uid.plus(ImmutableByteArray(16 - uid.size))
                blocks[0] = ClassicBlock(0, ClassicBlock.TYPE_MANUFACTURER, b0)

                val b1 = ImmutableByteArray(1).plus(NextfareTransitData.MANUFACTURER)
                        .plus(system_code).plus(ImmutableByteArray(1))
                blocks[1] = ClassicBlock(1, ClassicBlock.TYPE_DATA, b1)

                val b2: ImmutableByteArray
                if (block2 != null) {
                    b2 = block2.plus(ImmutableByteArray(16 - block2.size))
                } else
                    b2 = ImmutableByteArray(16)
                blocks[2] = ClassicBlock(2, ClassicBlock.TYPE_DATA, b2)
            } else {
                // TODO: Implement adding other data
                for (block_num in 0..2) {
                    val b = ByteArray(16)
                    blocks[block_num] = ClassicBlock(block_num, ClassicBlock.TYPE_DATA, b)
                }
            }

            blocks[3] = trailer
            sectors[sector_num] = ClassicSector(sector_num, blocks,
                    ClassicSectorKey.fromDump(
                            ImmutableByteArray.fromByteArray(MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY),
                            ClassicSectorKey.KeyType.A, "test"))
        }



        return ClassicCard(uid, Calendar.getInstance(), Arrays.asList<ClassicSector>(*sectors), false)
    }

    @Test
    fun testSeqGo() {
        // 0160 0012 3456 7893
        // This is a fake card number.
        val c1 = buildNextfareCard(ImmutableByteArray.fromHex("15cd5b07"),
                SeqGoTransitData.SYSTEM_CODE1, null)
        val d1 = c1.parseTransitData()
        assertTrue(message = "Card is seqgo", actual = d1 is SeqGoTransitData)
        assertEquals("0160 0012 3456 7893", d1.serialNumber)

        // 0160 0098 7654 3213
        // This is a fake card number.
        val c2 = buildNextfareCard(ImmutableByteArray.fromHex("b168de3a"),
                SeqGoTransitData.SYSTEM_CODE2, null)
        val d2 = c2.parseTransitData()
        assertTrue(message = "Card is seqgo", actual = d2 is SeqGoTransitData)
        assertEquals("0160 0098 7654 3213", d2.serialNumber)
    }

    @Test
    fun testLaxTap() {
        // 0160 0323 4663 8769
        // This is a fake card number (323.GO.METRO)
        val c = buildNextfareCard(ImmutableByteArray.fromHex("c40dcdc0"),
                ImmutableByteArray.fromHex("010101010101"), LaxTapTransitData.BLOCK2)
        val d = c.parseTransitData()
        assertTrue(message = "card is laxtap", actual = d is LaxTapTransitData)
        assertEquals("0160 0323 4663 8769", d.serialNumber)
    }

    @Test
    fun testMspGoTo() {
        // 0160 0112 3581 3212
        // This is a fake card number
        val c = buildNextfareCard(ImmutableByteArray.fromHex("897df842"),
                ImmutableByteArray.fromHex("010101010101"), MspGotoTransitData.BLOCK2)
        val d = c.parseTransitData() as NextfareTransitData?
        assertTrue(message = "card is mspgoto",
                actual = d is MspGotoTransitData)
        assertEquals("0160 0112 3581 3212", d.serialNumber)
    }
}
