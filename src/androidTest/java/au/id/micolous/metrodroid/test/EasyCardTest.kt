/*
 * EasyCardTest.kt
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

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransitData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * This test uses a EasyCard dump based on the one shown at:
 * http://www.fuzzysecurity.com/tutorials/rfid/4.html
 */
class EasyCardTest : CardReaderWithAssetDumpsTest(MfcCardImporter()) {

    @Test
    fun testdeadbeefEnglish() {
        setLocale("en-US")
        showRawStationIds(false)
        showLocalAndEnglish(false)

        val c = loadAndParseCard<ClassicCard, EasyCardTransitData>("easycard/deadbeef.mfc")
        assertEquals(TransitCurrency.TWD(245), c.balances!![0].balance)
        assertEquals(3, c.trips.size)

        val busTrip = c.trips[0]
        assertEquals("2013-10-28 20:33",
                (busTrip.startTimestamp as TimestampFull).isoDateTimeFormat())
        assertEquals(TransitCurrency.TWD(10), busTrip.fare)
        assertEquals(Trip.Mode.BUS, busTrip.mode)
        assertNull(busTrip.startStation)
        assertEquals("0x332211", busTrip.machineID)

        val trainTrip = c.trips[1]
        assertEquals("2013-10-28 20:41",
                (trainTrip.startTimestamp as TimestampFull).isoDateTimeFormat())
        assertEquals("2013-10-28 20:46",
                (trainTrip.endTimestamp as TimestampFull).isoDateTimeFormat())
        assertEquals(TransitCurrency.TWD(15), trainTrip.fare)
        assertEquals(Trip.Mode.METRO, trainTrip.mode)
        assertNotNull(trainTrip.startStation)
        assertEquals("Taipei Main Station", trainTrip.startStation!!.getStationName(false)?.unformatted)
        assertNotNull(trainTrip.endStation)
        assertEquals("NTU Hospital", trainTrip.endStation!!.getStationName(false)?.unformatted)
        assertNotNull(trainTrip.routeName?.unformatted)
        assertEquals("Red", trainTrip.routeName?.unformatted)
        assertEquals("0xccbbaa", trainTrip.machineID)

        val refill = c.trips[2]
        assertEquals("2013-07-27 08:58",
                (refill.startTimestamp as TimestampFull).isoDateTimeFormat())
        assertEquals(TransitCurrency.TWD(-100), refill.fare)
        assertEquals(Trip.Mode.TICKET_MACHINE, refill.mode)
        assertNotNull(refill.startStation)
        assertEquals("Yongan Market", refill.startStation!!.getStationName(false)?.unformatted)
        assertNull(refill.routeName)
        assertEquals("0x31c046", refill.machineID)
    }

    @Test
    fun testdeadbeefChineseTraditional() {
        setLocale("zh-TW")
        showRawStationIds(false)
        showLocalAndEnglish(false)

        val c = loadAndParseCard<ClassicCard, EasyCardTransitData>("easycard/deadbeef.mfc")
        val refill = c.trips.last()
        // Yongan Market
        assertEquals("永安市場", refill.startStation!!.getStationName(false)?.unformatted)
        assertNull(refill.routeName?.unformatted)
    }
}
