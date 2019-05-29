/*
 * MykiTest.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.files.RawDesfireFile
import au.id.micolous.metrodroid.transit.serialonly.MykiTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.*

class MykiTest {
    private fun constructMykiCardFromHexString(s: String): DesfireCard {
        val demoData = ImmutableByteArray.fromHex(s)

        // Construct a card to hold the data.
        val f = RawDesfireFile(ImmutableByteArray.fromHex("00000000000000"), demoData)
        val a = DesfireApplication(mapOf(15 to f), listOf())
        val a2 = DesfireApplication(mapOf(), listOf())
        return DesfireCard(ImmutableByteArray(32) { 0 },
                mapOf(MykiTransitData.APP_ID_1 to a, MykiTransitData.APP_ID_2 to a2))
    }

    @Test
    fun testDemoCard() {
        // This is mocked-up, incomplete data.
        val c = constructMykiCardFromHexString("C9B404004E61BC000000000000000000")

        // Check MIFARE AIDs
        assertEquals(2, c.applications.size)
        assertEquals(Pair(0x2110, 0), DesfireApplication.getMifareAID(MykiTransitData.APP_ID_1))
        assertEquals(Pair(0x210f, 0), DesfireApplication.getMifareAID(MykiTransitData.APP_ID_2))

        // Test TransitIdentity
        val i = c.parseTransitIdentity()
        assertEquals(MykiTransitData.NAME, i!!.name)
        assertEquals("308425123456780", i.serialNumber)

        // Test TransitData
        val d = c.parseTransitData()
        assertTrue(d is MykiTransitData, "TransitData must be instance of MykiTransitData")
        assertEquals("308425123456780", d.serialNumber)
    }
}
