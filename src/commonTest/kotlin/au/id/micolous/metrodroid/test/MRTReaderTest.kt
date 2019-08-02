/*
 * MTRReaderTest.kt
 *
 * Copyright 2018 Google
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

import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests StationTableReader (MdST). This uses the ezlink stop database.
 */

class MRTReaderTest : BaseInstrumentedTest() {
    @Test
    fun testGetStation() {
        setLocale("en-US")
        showRawStationIds(false)
        showLocalAndEnglish(false)

        val s = EZLinkTransitData.getStation("CGA")
        assertEquals("Changi Airport", s.getStationName(false)?.unformatted)
        assertNear(1.3575, s.latitude!!.toDouble(), 0.001)
        assertNear(103.9885, s.longitude!!.toDouble(), 0.001)
    }
}
