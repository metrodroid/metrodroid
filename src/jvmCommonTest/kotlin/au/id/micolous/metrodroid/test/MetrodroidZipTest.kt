/*
* MetrodroidZipTest.kt
*
* Copyright 2021 Google
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
import au.id.micolous.metrodroid.serializers.XmlOrJsonCardFormat
import java.util.*
import kotlin.test.*

/**
 * Contains tests for the Metrodroid Zip format.
 */

class MetrodroidZipTest : BaseInstrumentedTest() {
    private var oldTz: TimeZone? = null
    @BeforeTest
    fun setUp() {
        oldTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
    }

    @AfterTest
    fun tearDown() {
        TimeZone.setDefault(oldTz)
    }

    @Test
    fun testMetrodroidZip() {
        val stream = loadAssetStream("zip/metrodroid.zip")
        assertNotNull(stream, "Couldn't open zip/metrodroid.zip")
        val cards = XmlOrJsonCardFormat().readCards(
            stream
        )!!.asSequence().toList()
        assertEquals(9, cards.size)
        for ((ctr, card) in cards.withIndex()) {
            val json = JsonKotlinFormat.makeCardString(card)
            Log.d("MetrodroidZipTest", "reserial[$ctr] = $json")
            val expected = loadSmallAssetBytes("zip/metrodroid_$ctr.json")
            assertEquals(
                expected = expected.decodeToString().trim(),
                actual = json.trim(),
                message = "Wrong reserialization for card $ctr"
            )
        }
    }
}