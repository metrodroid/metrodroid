/*
 * ClassicCardTest.kt
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
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClassicCardTest: CardReaderWithAssetDumpsTest<JsonKotlinFormat>(JsonKotlinFormat) {
    @Test
    fun testIncomplete() {
        val incompleteFiles = arrayOf(
                "mfc/mfc-incomplete0.json",
                "mfc/mfc-incomplete1.json")

        for (path in incompleteFiles) {
            val c = loadCard<ClassicCard>(path)
            val classic = c.mifareClassic
            assertNotNull(classic, path)
            assertNull(c.manufacturingInfo, path)
            assertNull(classic.manufacturingInfo, path)
            assertEquals(1, classic.sectors.size, path)
            assertEquals(0, classic.sectors[0].blocks.size, path)
        }
    }
}
