/*
 * OpalTest.kt
 *
 * Copyright 2017-2018 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.opal.OpalData
import au.id.micolous.metrodroid.transit.opal.OpalTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.*

/**
 * Tests for Opal card
 */
class OpalTest {
    private fun constructOpalCardFromHexString(s: String): DesfireCard {
        val demoData = ImmutableByteArray.fromHex(s)

        // Construct a card to hold the data.
        val f = DesfireFile.create(ImmutableByteArray.fromHex("00000000000000"), demoData)
        val a = DesfireApplication(mapOf(OpalTransitData.FILE_ID to f.raw), emptyList())
        val c = DesfireCard(
                ImmutableByteArray.empty(),
                mapOf(OpalTransitData.APP_ID to a))
        assertEquals(1, c.applications.size)
        assertNull(message = "Opal shouldn't have a valid AID",
                actual = DesfireApplication.getMifareAID(OpalTransitData.APP_ID))
        return c
    }

    @Test
    fun testDemoCard() {
        // This is mocked-up data, probably has a wrong checksum.
        val c = constructOpalCardFromHexString("87d61200e004002a0014cc44a4133930")

        // Test TransitIdentity
        val i = c.parseTransitIdentity()
        assertNotNull(i)
        assertEquals(OpalTransitData.CARD_INFO.name, i.name)
        assertEquals("3085 2200 1234 5670", i.serialNumber)

        // Test TransitData
        val d = c.parseTransitData()
        assertTrue(d is OpalTransitData, "TransitData must be instance of OpalTransitData")

        val o = d as OpalTransitData?
        assertEquals("3085 2200 1234 5670", o!!.serialNumber)
        assertEquals(TransitCurrency.AUD(336), o.balance)
        assertEquals(0, o.subscriptions!!.size)
        // 2015-10-05 09:06 UTC+11
        assertEquals("2015-10-04 22:06", o.lastTransactionTime.isoDateTimeFormat())
        assertEquals(OpalData.MODE_BUS, o.lastTransactionMode)
        assertEquals(OpalData.ACTION_JOURNEY_COMPLETED_DISTANCE, o.lastTransaction)
        assertEquals(39, o.lastTransactionNumber)
        assertEquals(1, o.weeklyTrips)
    }

    @Test
    fun testDaylightSavings() {
        // This is all mocked-up data, probably has a wrong checksum.

        // 2018-03-31 09:00 UTC+11
        // 2018-03-30 22:00 UTC
        var c = constructOpalCardFromHexString("85D25E07230520A70044DA380419FFFF")

        var o: OpalTransitData = c.parseTransitData() as OpalTransitData
        assertEquals("2018-03-30 22:00", o.lastTransactionTime.isoDateTimeFormat())

        // DST transition is at 2018-04-01 03:00

        // 2018-04-01 09:00 UTC+10
        // 2018-03-31 23:00 UTC
        c = constructOpalCardFromHexString("85D25E07430520A70048DA380419FFFF")

        o = c.parseTransitData() as OpalTransitData
        assertEquals("2018-03-31 23:00", o.lastTransactionTime.isoDateTimeFormat())
    }

}
