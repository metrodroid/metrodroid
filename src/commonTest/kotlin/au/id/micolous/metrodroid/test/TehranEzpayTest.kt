/*
 * Copyright 2026 Metrodroid contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.tehran_ezpay.TehranEzpayRecord
import au.id.micolous.metrodroid.transit.tehran_ezpay.TehranEzpayTransitData
import au.id.micolous.metrodroid.transit.tehran_ezpay.TehranEzpayTransitFactory
import au.id.micolous.metrodroid.transit.tehran_ezpay.isTehranEzpayCounterNewer
import au.id.micolous.metrodroid.transit.tehran_ezpay.selectTehranEzpayRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TehranEzpayTest : CardReaderWithAssetDumpsTest<JsonKotlinFormat>(JsonKotlinFormat) {
    private fun copyWithBlock(card: ClassicCard, sector: Int, block: Int,
                              data: ImmutableByteArray): ClassicCard {
        val sectors = card.sectorsRaw.toMutableList()
        val blocks = sectors[sector].blocks.toMutableList()
        blocks[block] = data
        sectors[sector] = sectors[sector].copy(blocks = blocks)
        return ClassicCard(sectors, card.subType, card.isPartialRead).also {
            it.postCreate(Card(card.tagId, card.scannedAt, mifareClassic = it))
        }
    }

    private fun checkDump(path: String, expectedBalance: Int, expectedCounter: Long) {
        val card = loadCard<ClassicCard>(path)
        val data = card.parseTransitData()
        assertIs<TehranEzpayTransitData>(data)
        assertEquals("Tehran Ezpay", data.cardName)
        assertEquals("D319460F83", data.serialNumber)
        assertEquals(TransitCurrency(expectedBalance, "IRR", 1), data.balance)
        assertEquals(expectedCounter, data.recordCounter)
    }

    @Test
    fun testSequentialDumps() {
        setLocale("en-US")
        checkDump("tehran_ezpay/before-entry.json", 660332, 0xed)
        checkDump("tehran_ezpay/after-entry.json", 587332, 0xee)
        checkDump("tehran_ezpay/after-exit.json", 514332, 0xef)
    }

    @Test
    fun testAlternatingRecordSelection() {
        val older = TehranEzpayRecord(0xed, 660332)
        val newer = TehranEzpayRecord(0xee, 587332)
        assertEquals(newer, selectTehranEzpayRecord(older, newer))
        assertEquals(newer, selectTehranEzpayRecord(newer, older))
    }

    @Test
    fun testCounterWraparound() {
        assertTrue(isTehranEzpayCounterNewer(0xee, 0xed))
        assertTrue(isTehranEzpayCounterNewer(0xef, 0xee))
        assertTrue(isTehranEzpayCounterNewer(0, 0xffffffffL))
        assertFalse(isTehranEzpayCounterNewer(0xffffffffL, 0))
        assertFalse(isTehranEzpayCounterNewer(1, 1))
        assertFalse(isTehranEzpayCounterNewer(0x80000000L, 0))
        assertEquals(TehranEzpayRecord(0, 2), selectTehranEzpayRecord(
                TehranEzpayRecord(0xffffffffL, 1), TehranEzpayRecord(0, 2)))
    }

    @Test
    fun testRejectsOtherAndIncompleteClassicCards() {
        val other = loadCard<ClassicCard>("selecta/selecta.json").mifareClassic!!
        assertFalse(TehranEzpayTransitFactory.check(other))

        val incomplete = loadCard<ClassicCard>("mfc/mfc-incomplete0.json").mifareClassic!!
        assertFalse(TehranEzpayTransitFactory.check(incomplete))
    }

    @Test
    fun testRequiredAndOptionalBlocks() {
        val card = loadCard<ClassicCard>("tehran_ezpay/after-entry.json")
        // This captured dump already has unread, unrelated blocks in sector 0.
        assertTrue(TehranEzpayTransitFactory.check(card))

        val noJourney = copyWithBlock(card, 3, 1, ImmutableByteArray.empty())
        assertTrue(TehranEzpayTransitFactory.check(noJourney))
        val data = noJourney.parseTransitData()
        assertIs<TehranEzpayTransitData>(data)
        assertEquals(TransitCurrency(587332, "IRR", 1), data.balance)

        assertFalse(TehranEzpayTransitFactory.check(
                copyWithBlock(card, 4, 0, ImmutableByteArray.empty())))
        assertFalse(TehranEzpayTransitFactory.check(
                copyWithBlock(card, 5, 0, ImmutableByteArray.fromHex("00"))))
    }

    @Test
    fun testIdentificationFields() {
        val card = loadCard<ClassicCard>("tehran_ezpay/before-entry.json")
        assertEquals("D319460F83", TehranEzpayTransitFactory.parseTransitIdentity(card).serialNumber)

        val badBcc = card.sectorsRaw[0].blocks[0].dataCopy.also { it[4] = 0 }
        assertFalse(TehranEzpayTransitFactory.check(
                copyWithBlock(card, 0, 0, ImmutableByteArray.fromByteArray(badBcc))))

        val badReversedUid = card.sectorsRaw[3].blocks[0].dataCopy.also { it[2] = 0 }
        assertFalse(TehranEzpayTransitFactory.check(
                copyWithBlock(card, 3, 0, ImmutableByteArray.fromByteArray(badReversedUid))))
    }

    @Test
    fun testRawFields() {
        setLocale("en-US")
        val data = loadCard<ClassicCard>("tehran_ezpay/after-entry.json").parseTransitData()
        assertIs<TehranEzpayTransitData>(data)
        val fields = data.getRawFields(TransitData.RawLevel.ALL)
        assertEquals("Record counter", fields[0].text1?.unformatted)
        assertEquals("238 (0xEE)", fields[0].text2?.unformatted)
        assertEquals("Journey state", fields[1].text1?.unformatted)
        assertEquals("Entered / journey open", fields[1].text2?.unformatted)
    }
}
