/*
 * SuicaTest.kt
 *
 * Copyright 2020 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.transit.suica.SuicaUtil
import au.id.micolous.metrodroid.multi.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SuicaTest {
    /*
     * Tests for card identification by Service IDs.
     *
     * These are all complete lists of service IDs from cards in the wild.
     */
    @Test
    fun testIdentifyHayakaken() {
        assertEquals(R.string.card_name_hayakaken, SuicaUtil.getCardName(setOf(
            0x48, 0x4a, 0x88, 0x8b, 0xc8, 0xca, 0xcc, 0xce, 0xd0, 0xd2, 0xd4, 0xd6, 0x810, 0x812,
            0x816, 0x850, 0x852, 0x856, 0x890, 0x892, 0x896, 0x8c8, 0x8ca, 0x90a, 0x90c, 0x90f, 0x1008,
            0x100a, 0x1048, 0x104a, 0x108c, 0x108f, 0x10c8, 0x10cb, 0x1108, 0x110a, 0x1148, 0x114a,
            0x1f88, 0x1f8a, 0x2048, 0x204a, 0x2448, 0x244a, 0x2488, 0x248a, 0x24c8, 0x24ca, 0x2508,
            0x250a, 0x2548, 0x254a)))
    }

    @Test
    fun testIdentifyIcoca() {
        assertEquals(R.string.card_name_icoca, SuicaUtil.getCardName(setOf(
            0x48, 0x4a, 0x88, 0x8b, 0xc8, 0xca, 0xcc, 0xce, 0xd0, 0xd2, 0xd4, 0xd6, 0x810, 0x812,
            0x816, 0x850, 0x852, 0x856, 0x890, 0x892, 0x896, 0x8c8, 0x8ca, 0x90c, 0x90f, 0x1008,
            0x100a, 0x1048, 0x104a, 0x108c, 0x108f, 0x10c8, 0x10cb, 0x1108, 0x110a, 0x1148, 0x114a,
            0x1a48, 0x1a4a, 0x1a88, 0x1a8a, 0x9608, 0x960a)))
    }

    @Test
    fun testIdentifyNimoca() {
        assertEquals(R.string.card_name_nimoca, SuicaUtil.getCardName(setOf(
            0x48, 0x4a, 0x88, 0x8b, 0xc8, 0xca, 0xcc, 0xce, 0xd0, 0xd2, 0xd4, 0xd6, 0x810, 0x812,
            0x816, 0x850, 0x852, 0x856, 0x890, 0x892, 0x896, 0x8c8, 0x8ca, 0x90a, 0x90c, 0x90f, 0x1008,
            0x100a, 0x1048, 0x104a, 0x108c, 0x108f, 0x10c8, 0x10cb, 0x1108, 0x110a, 0x1148, 0x114a,
            0x1f48, 0x1f4a, 0x1f88, 0x1f8a, 0x1fc8, 0x1fca, 0x2008, 0x200a, 0x2048, 0x204a)))
    }

    @Test
    fun testIdentifyPasmo() {
        assertEquals(R.string.card_name_pasmo, SuicaUtil.getCardName(setOf(
            0x48, 0x4a, 0x88, 0x8b, 0x810, 0x812, 0x816, 0x850, 0x852, 0x856, 0x890, 0x892, 0x896,
            0x8c8, 0x8ca, 0x90a, 0x90c, 0x90f, 0x1008, 0x100a, 0x1048, 0x104a, 0x108c, 0x108f, 0x10c8,
            0x10cb, 0x1108, 0x110a, 0x1148, 0x114a, 0x1848, 0x184b, 0x1908, 0x190a, 0x1948, 0x194b,
            0x1988, 0x198b, 0x1cc8, 0x1cca, 0x1d08, 0x1d0a, 0x2308, 0x230a, 0x2348, 0x234b, 0x2388,
            0x238b, 0x23c8, 0x23cb)))
    }

    @Test
    fun testIdentifySuica() {
        assertEquals(R.string.card_name_suica, SuicaUtil.getCardName(setOf(
            0x48, 0x4a, 0x88, 0x8b, 0xc8, 0xca, 0xcc, 0xce, 0xd0, 0xd2, 0xd4, 0xd6, 0x810, 0x812,
            0x816, 0x850, 0x852, 0x856, 0x890, 0x892, 0x896, 0x8c8, 0x8ca, 0x90a, 0x90c, 0x90f, 0x1008,
            0x100a, 0x1048, 0x104a, 0x108c, 0x108f, 0x10c8, 0x10cb, 0x1108, 0x110a, 0x1148, 0x114a,
            0x1808, 0x180a, 0x1848, 0x184b, 0x18c8, 0x18ca, 0x1908, 0x190a, 0x1948, 0x194b, 0x1988,
            0x198b, 0x2308, 0x230a, 0x2348, 0x234b, 0x2388, 0x238b, 0x23c8, 0x23cb)))
    }

    /*
     * Ambiguous service ID lists.
     *
     * These are from older versions of Metrodroid that didn't record the locked service IDs.
     */
    @Test
    fun testIdenitfyAmbiguous() {
        // Hayakaken and ICOCA both have only these open services
        assertNull(SuicaUtil.getCardName(setOf(
            0x8b, 0x90f, 0x108f, 0x10cb)))

        // PASMO and Suica both only have these open services
        assertNull(SuicaUtil.getCardName(setOf(
            0x8b, 0x90f, 0x108f, 0x10cb, 0x184b, 0x194b, 0x234b, 0x238b, 0x23cb)))

    }
}
