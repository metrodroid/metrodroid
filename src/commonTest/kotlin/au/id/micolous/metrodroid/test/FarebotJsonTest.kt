/*
 * FarebotJsonTest.kt
 *
 * Copyright 2019 Google
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

import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.serializers.AutoJsonFormat
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contains tests for the Farebot Json format.
 */
class FarebotJsonTest : CardReaderWithAssetDumpsTest<AutoJsonFormat>(AutoJsonFormat) {
    @Test
    fun testFarebotJson() {
        val cards = importer.readCardList(
            loadAsset("farebot/farebot.json").readToString())
        assertEquals(7, cards.size)
        for ((ctr, card) in cards.withIndex()) {
            val json = JsonKotlinFormat.makeCardString(card)
            Log.d("FarebotJsonTest", "reserial[$ctr] = $json")
            val expected = loadSmallAssetBytes("farebot/metrodroid_$ctr.json")
            assertEquals(expected = expected.decodeToString().trim(),
                         actual = json.trim(),
                         message = "Wrong reserialization for card $ctr")
        }
    }
}
