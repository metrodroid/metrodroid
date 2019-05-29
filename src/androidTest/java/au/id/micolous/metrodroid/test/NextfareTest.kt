/*
 * NextfareTest.kt
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

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.ClassicSectorRaw
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData
import au.id.micolous.metrodroid.transit.msp_goto.MspGotoTransitData
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData
import au.id.micolous.metrodroid.transit.nextfare.NextfareUnknownTransitData
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareBalanceRecord
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareConfigRecord
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests relating to Cubic Nextfare reader.
 */
class NextfareTest {
    @Test
    fun testExpiryDate() {
        val r20250602 = ImmutableByteArray.fromHex("01030000c2320000010200000000bf0c")
        val r20240925 = ImmutableByteArray.fromHex("0103000039310000010200000000924a")
        val r20180815 = ImmutableByteArray.fromHex("010300010f25000004000000fb75c2f7")

        val r1 = NextfareConfigRecord.recordFromBytes(r20250602, MetroTimeZone.UTC)
        assertEquals("2025-06-02 00:00", (r1.expiry as TimestampFull).isoDateTimeFormat())

        val r2 = NextfareConfigRecord.recordFromBytes(r20240925, MetroTimeZone.UTC)
        assertEquals("2024-09-25 00:00", (r2.expiry as TimestampFull).isoDateTimeFormat())

        val r3 = NextfareConfigRecord.recordFromBytes(r20180815, MetroTimeZone.UTC)
        assertEquals("2018-08-15 00:00", (r3.expiry as TimestampFull).isoDateTimeFormat())
    }

    @Test
    fun testTransactionRecord() {
        val rnull = ImmutableByteArray.fromHex("01000000000000000000000000007f28")

        val r = NextfareTransactionRecord.recordFromBytes(rnull, MetroTimeZone.UTC)
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

        val trailer = ImmutableByteArray.fromHex("ffffffffffff78778800a1a2a3a4a5a6")

        val sectors = mutableListOf<ClassicSector>()

        val b2sane = block2 ?: ImmutableByteArray.empty()

        sectors += ClassicSector.create(ClassicSectorRaw(
                listOf(
                        uid + ImmutableByteArray(16 - uid.size),
                        ImmutableByteArray(1) + NextfareTransitData.MANUFACTURER +
                                system_code + ImmutableByteArray(1),
                        b2sane + ImmutableByteArray(16 - b2sane.size), trailer),
                ImmutableByteArray.fromHex("ffffffffffff"),
                null, false, null))

        for (sector_num in 1..15) {
            sectors += ClassicSector.create(ClassicSectorRaw(
                    (0..2).map { ImmutableByteArray(16) }
                            + listOf(trailer),
                    ImmutableByteArray.fromHex("ffffffffffff"),
                    null, false, null))
        }

        return ClassicCard(sectors)
    }

    @Test
    fun testSeqGo() {
        // 0160 0012 3456 7893
        // This is a fake card number.
        val c1 = buildNextfareCard(ImmutableByteArray.fromHex("15cd5b07"),
                SeqGoTransitData.SYSTEM_CODE1, null)
        val d1 = c1.parseTransitData()
        assertTrue(d1 is SeqGoTransitData, "Card is seqgo")
        assertEquals("0160 0012 3456 7893", d1.serialNumber)
        assertEquals("AUD", d1.balance?.balance?.mCurrencyCode)

        // 0160 0098 7654 3213
        // This is a fake card number.
        val c2 = buildNextfareCard(ImmutableByteArray.fromHex("b168de3a"),
                SeqGoTransitData.SYSTEM_CODE2, null)
        val d2 = c2.parseTransitData()
        assertTrue(d2 is SeqGoTransitData, "Card is seqgo")
        assertEquals("0160 0098 7654 3213", d2.serialNumber)
        assertEquals("AUD", d2.balance?.balance?.mCurrencyCode)
    }

    @Test
    fun testLaxTap() {
        // 0160 0323 4663 8769
        // This is a fake card number (323.GO.METRO)
        val c = buildNextfareCard(ImmutableByteArray.fromHex("c40dcdc0"),
                ImmutableByteArray.fromHex("010101010101"), LaxTapTransitData.BLOCK2)
        val d = c.parseTransitData() as NextfareTransitData?
        assertTrue(d is LaxTapTransitData, "card is laxtap")
        assertEquals("0160 0323 4663 8769", d.serialNumber)
        assertEquals("USD", d.balance?.balance?.mCurrencyCode)
    }

    @Test
    fun testMspGoTo() {
        // 0160 0112 3581 3212
        // This is a fake card number
        val c = buildNextfareCard(ImmutableByteArray.fromHex("897df842"),
                ImmutableByteArray.fromHex("010101010101"), MspGotoTransitData.BLOCK2)
        val d = c.parseTransitData() as NextfareTransitData?
        assertTrue(d is MspGotoTransitData, "card is mspgoto")
        assertEquals("0160 0112 3581 3212", d.serialNumber)
        assertEquals("USD", d.balance?.balance?.mCurrencyCode)
    }

    @Test
    fun testUnknownCard() {
        // 0160 0112 3581 3212
        // This is a fake card number
        val c1 = buildNextfareCard(ImmutableByteArray.fromHex("897df842"),
                ImmutableByteArray.fromHex("010101010101"),
                ImmutableByteArray.fromHex("ff00ff00ff00ff00ff00ff00ff00ff00"))
        val d1 = c1.parseTransitData() as NextfareTransitData?
        assertTrue(d1 is NextfareUnknownTransitData, "card is unknown nextfare")
        assertEquals("0160 0112 3581 3212", d1.serialNumber)
        assertEquals("XXX", d1.balance?.balance?.mCurrencyCode)

        val c2 = buildNextfareCard(ImmutableByteArray.fromHex("897df842"),
                ImmutableByteArray.fromHex("ff00ff00ff00"),
                null)
        val d2 = c2.parseTransitData() as NextfareTransitData?
        assertTrue(d2 is NextfareUnknownTransitData, "card is unknown nextfare")
        assertEquals("0160 0112 3581 3212", d2.serialNumber)
        assertEquals("XXX", d2.balance?.balance?.mCurrencyCode)
    }
}
