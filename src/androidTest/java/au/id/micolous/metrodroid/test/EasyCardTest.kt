/*
 * EasyCardTest.java
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

import android.test.InstrumentationTestCase
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransitData
import au.id.micolous.metrodroid.util.Utils

/**
 * This test uses a EasyCard dump from: http://www.fuzzysecurity.com/tutorials/rfid/4.html
 */
class EasyCardTest : InstrumentationTestCase() {
    private fun parseCard(c: ClassicCard): EasyCardTransitData {
        val d = c.parseTransitData()
        assertNotNull("Transit data not parsed", d)
        assertTrue(d is EasyCardTransitData)
        return d as EasyCardTransitData
    }

    private fun loadCard(path: String): EasyCardTransitData {
        return parseCard(TestUtils.loadMifareClassic1KFromAssets(instrumentation.context, path))
    }

    fun testdeadbeefEnglish() {
        TestUtils.setLocale(instrumentation.context, "en-US")
        TestUtils.showRawStationIds(false)
        TestUtils.showLocalAndEnglish(false)

        val c = loadCard("easycard/deadbeef.mfc")
        assertEquals(TransitCurrency.TWD(245), c.balances!![0].balance)
        assertEquals(2, c.trips.size)

        val trip = c.trips[0]
        assertEquals("2013-10-28 20:41",
                Utils.isoDateTimeFormat(trip.startTimestamp))
        assertEquals(TransitCurrency.TWD(15), trip.fare)
        assertEquals(Trip.Mode.BUS, trip.mode)
        assertNull(trip.startStation)
        assertEquals("0x1233a4", trip.machineID)

        val refill = c.trips[1]
        assertEquals("2013-07-27 08:58",
                Utils.isoDateTimeFormat(refill.startTimestamp))
        assertEquals(TransitCurrency.TWD(-100), refill.fare)
        assertEquals(Trip.Mode.TICKET_MACHINE, refill.mode)
        assertNotNull(refill.startStation)
        assertEquals("Yongan Market", refill.startStation!!.stationName)
        assertNull(refill.routeName)
        assertEquals("0x31c046", refill.machineID)
    }

    fun testdeadbeefChineseTraditional() {
        TestUtils.setLocale(instrumentation.context, "zh-TW")
        TestUtils.showRawStationIds(false)
        TestUtils.showLocalAndEnglish(false)

        val c = loadCard("easycard/deadbeef.mfc")
        val refill = c.trips[1]
        // Yongan Market
        assertEquals("永安市場", refill.startStation!!.stationName)
        assertNull(refill.routeName)
    }
}
