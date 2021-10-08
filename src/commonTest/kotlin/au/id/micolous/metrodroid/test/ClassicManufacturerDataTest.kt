/*
 * ClassicManufacturerDataTest.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.card.classic.ClassicCard.Companion.MANUFACTURER_FUDAN
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import kotlin.test.Test
import kotlin.test.assertNotNull

class ClassicManufacturerDataTest: CardReaderWithAssetDumpsTest<MfcCardImporter>(MfcCardImporter()) {
    @Test
    fun testFudan() {
        // Dump is from Bilhete Unico tests
        val c = loadCard<ClassicCard>("9e4937b0.mfd")
        assertNotNull(c.mifareClassic)

        val mi = c.manufacturingInfo
        assertNotNull(mi)
        assertContainsListItem(MANUFACTURER_FUDAN, mi)
    }

    @Test
    fun testNotFudan() {
        // Dump is from Bilhete Unico tests
        val c = loadCard<ClassicCard>("7eb2258a.mfd")
        assertNotNull(c.mifareClassic)

        val mi = c.manufacturingInfo
        assertNotNull(mi)
        assertNotContainsListItem(MANUFACTURER_FUDAN, mi)
    }
}