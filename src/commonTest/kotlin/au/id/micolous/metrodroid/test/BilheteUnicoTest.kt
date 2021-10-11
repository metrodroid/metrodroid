/*
 * BilheteUnicoTest.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.bilhete_unico.BilheteUnicoSPTransitData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for Bilhete Unico.
 */
class BilheteUnicoTest : CardReaderWithAssetDumpsTest<MfcCardImporter>(MfcCardImporter()) {

    private fun balanceTest(path: String, expectedBalance: TransitCurrency) {
        // We don't have much info on these cards. Most of these tests are the same.
        val actualBalance = loadAndParseCard<ClassicCard, BilheteUnicoSPTransitData>(path).balance
        assertNotNull(actualBalance)
        assertEquals(expectedBalance, actualBalance)
    }

    // These tests are all based on dumps from https://github.com/vpereira/bilhete
    //
    // The dumps are from 2011, so the system may have changed more recently. The values aren't
    // documented, so we brute-forced them for the test.
    //
    // TODO: Add some more checks around these files, as we learn more about BU.

    @Test
    fun test7eb2258a() {
        balanceTest("7eb2258a.mfd", TransitCurrency.BRL(2400))
        balanceTest("7eb2258a/201111242210.dump", TransitCurrency.BRL(2400))
        balanceTest("7eb2258a/201111272000.dump", TransitCurrency.BRL(1800))
        balanceTest("7eb2258a/201111282115.dump", TransitCurrency.BRL(1200))
    }

    @Test
    fun test9e4937b0() {
        balanceTest("9e4937b0.mfd", TransitCurrency.BRL(592))
        balanceTest("9e4937b0/201111241013.dump", TransitCurrency.BRL(592))
        balanceTest("9e4937b0/201111281500.dump", TransitCurrency.BRL(292))
        balanceTest("9e4937b0/201111281515.dump", TransitCurrency.BRL(292))
        balanceTest("9e4937b0/201112011015.dump", TransitCurrency.BRL(1002))
        balanceTest("9e4937b0/201112151145.dump", TransitCurrency.BRL(702))
        balanceTest("9e4937b0/201112191024.dump", TransitCurrency.BRL(402))
        balanceTest("9e4937b0/201112191036.dump", TransitCurrency.BRL(402))
    }

    @Test
    fun testfcd4cf1f() {
        balanceTest("fcd4cf1f.mfd", TransitCurrency.BRL(1000))
        balanceTest("fcd4cf1f/201111241013.dump", TransitCurrency.BRL(1000))
        balanceTest("fcd4cf1f/201112011030.dump", TransitCurrency.BRL(215))
    }
}

