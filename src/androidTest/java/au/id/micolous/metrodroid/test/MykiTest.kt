/*
 * MykiTest.java
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
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile
import au.id.micolous.metrodroid.transit.serialonly.MykiTransitData
import au.id.micolous.metrodroid.util.Utils
import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class MykiTest {
    private fun constructMykiCardFromHexString(s: String): DesfireCard {
        val demoData = Utils.hexStringToByteArray(s)

        // Construct a card to hold the data.
        val f = DesfireFile.create(15, null, demoData)
        val a = DesfireApplication(MykiTransitData.APP_ID_1, arrayOf(f))
        val a2 = DesfireApplication(MykiTransitData.APP_ID_2, arrayOf())
        return DesfireCard(byteArrayOf(0, 1, 2, 3),
                Calendar.getInstance(), null,
                arrayOf(a, a2))
    }

    @Test
    fun testDemoCard() {
        // This is mocked-up, incomplete data.
        val c = constructMykiCardFromHexString("C9B404004E61BC000000000000000000")

        // Test TransitIdentity
        val i = c.parseTransitIdentity()
        assertEquals(MykiTransitData.NAME, i!!.name)
        Assert.assertEquals("308425123456780", i.serialNumber)

        // Test TransitData
        val d = c.parseTransitData()
        Assert.assertTrue("TransitData must be instance of MykiTransitData", d is MykiTransitData)

        val o = d as MykiTransitData
        Assert.assertEquals("308425123456780", o.serialNumber)
    }
}
