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
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.transit.TransitCurrency
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
        assertTrue(isTehranEzpayCounterNewer(0, 0xffffffffL))
        assertFalse(isTehranEzpayCounterNewer(0xffffffffL, 0))
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
}
