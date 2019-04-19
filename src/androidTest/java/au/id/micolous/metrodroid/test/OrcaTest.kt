/*
 * OrcaTest.java
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

import java.util.Calendar

import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile
import au.id.micolous.metrodroid.card.desfire.settings.RecordDesfireFileSettings
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.orca.OrcaTransitData
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.ImmutableByteArray

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Tests for Orca card
 */

class OrcaTest : BaseInstrumentedTest() {

    private fun constructOrcaCard(): DesfireCard {
        // Construct a card to hold the data.
        val f2 = RecordDesfireFile.create(2,
                RecordDesfireFileSettings(0.toByte(), 0.toByte(), ImmutableByteArray.empty(),
                        48, 5, 5),
                ImmutableByteArray.fromHex(testFile0x2))
        val f4 = DesfireFile.create(4, null, ImmutableByteArray.fromHex(testFile0x4))
        val ff = DesfireFile.create(15, null,
                ImmutableByteArray.fromHex(testFile0xf))
        val a = DesfireApplication(OrcaTransitData.APP_ID, listOf(f2, f4))
        val a2 = DesfireApplication(0xffffff, listOf(ff))
        return DesfireCard(ImmutableByteArray.fromHex("00010203"),
                Calendar.getInstance(), null,
                listOf(a, a2))
    }

    @Test
    fun testDemoCard() {
        setLocale("en-US")
        showRawStationIds(false)

        // This is mocked-up data, probably has a wrong checksum.
        val c = constructOrcaCard()

        // Test TransitIdentity
        val i = c.parseTransitIdentity()
        assertEquals("ORCA", i!!.name)
        assertEquals("12030625", i.serialNumber)

        val d = c.parseTransitData()
        assertTrue(message = "TransitData must be instance of OrcaTransitData",
                actual = d is OrcaTransitData)

        assertEquals("12030625", d.serialNumber)
        assertEquals("ORCA", d.cardName)
        assertEquals(TransitCurrency.USD(23432), d.balance)
        assertNull(d.subscriptions)

        val trips = d.trips?.sortedWith(Trip.Comparator())
        assertNotNull(trips)
        assertEquals("Community Transit", trips[0].getAgencyName(false))
        assertEquals("CT", trips[0].getAgencyName(true))
        assertEquals((1514843334L + 256) * 1000, trips[0].startTimestamp!!.timeInMillis)
        assertEquals(TransitCurrency.USD(534), trips[0].fare)
        assertNull(trips[0].routeName)
        assertTrue(trips[0].hasTime())
        assertEquals(Trip.Mode.BUS, trips[0].mode)
        assertNull(trips[0].startStation)
        assertNull(trips[0].endStation)
        assertEquals("30246", trips[0].vehicleID)

        assertEquals("Unknown (0xf)", trips[1].getAgencyName(false))
        assertEquals("Unknown (0xf)", trips[1].getAgencyName(true))
        assertEquals(1514843334L * 1000, trips[1].startTimestamp!!.timeInMillis)
        assertEquals(TransitCurrency.USD(289), trips[1].fare)
        assertNull(trips[1].routeName)
        assertTrue(trips[1].hasTime())
        assertEquals(Trip.Mode.BUS, trips[1].mode)
        assertNull(trips[1].startStation)
        assertNull(trips[1].endStation)
        assertEquals("30262", trips[1].vehicleID)

        assertEquals("Sound Transit", trips[2].getAgencyName(false))
        assertEquals("ST", trips[2].getAgencyName(true))
        assertEquals((1514843334L - 256) * 1000, trips[2].startTimestamp!!.timeInMillis)
        assertEquals(TransitCurrency.USD(179), trips[2].fare)
        assertEquals("Link Light Rail", trips[2].routeName)
        assertTrue(trips[2].hasTime())
        assertEquals(Trip.Mode.METRO, trips[2].mode)
        assertNotNull(trips[2].startStation)
        assertEquals("Stadium", trips[2].startStation!!.stationName)
        assertEquals("Stadium", trips[2].startStation!!.shortStationName)
        assertNear(47.5918121, java.lang.Float.parseFloat(trips[2].startStation!!.latitude).toDouble(), 0.00001)
        assertNear(-122.327354, java.lang.Float.parseFloat(trips[2].startStation!!.longitude).toDouble(), 0.00001)
        assertNull(trips[2].endStation)

        assertEquals("Sound Transit", trips[3].getAgencyName(false))
        assertEquals("ST", trips[3].getAgencyName(true))
        assertEquals((1514843334L - 512) * 1000, trips[3].startTimestamp!!.timeInMillis)
        assertEquals(TransitCurrency.USD(178), trips[3].fare)
        assertEquals("Sounder Train", trips[3].routeName)
        assertTrue(trips[3].hasTime())
        assertEquals(Trip.Mode.TRAIN, trips[3].mode)
        assertNotNull(trips[3].startStation)
        assertEquals("King Street", trips[3].startStation!!.stationName)
        assertEquals("King St", trips[3].startStation!!.shortStationName)
        assertNear(47.598445, java.lang.Float.parseFloat(trips[3].startStation!!.latitude).toDouble(), 0.00001)
        assertNear(-122.330161, java.lang.Float.parseFloat(trips[3].startStation!!.longitude).toDouble(), 0.00001)
        assertNull(trips[3].endStation)

        assertEquals("Washington State Ferries", trips[4].getAgencyName(false))
        assertEquals("WSF", trips[4].getAgencyName(true))
        assertEquals((1514843334L - 768) * 1000, trips[4].startTimestamp!!.timeInMillis)
        assertEquals(TransitCurrency.USD(177), trips[4].fare)
        assertNull(trips[4].routeName)
        assertTrue(trips[4].hasTime())
        assertEquals(Trip.Mode.FERRY, trips[4].mode)
        assertNotNull(trips[4].startStation)
        assertEquals("Seattle Terminal", trips[4].startStation!!.stationName)
        assertEquals("Seattle", trips[4].startStation!!.shortStationName)
        assertNear(47.602722, java.lang.Float.parseFloat(trips[4].startStation!!.latitude).toDouble(), 0.00001)
        assertNear(-122.338512, java.lang.Float.parseFloat(trips[4].startStation!!.longitude).toDouble(), 0.00001)
        assertNull(trips[4].endStation)
    }

    companion object {

        // mocked data
        private const val record0 = "00000025a4aadc6000076260000000042c00000000000000000000000000" + "000000000000000000000000000000000000"
        private const val record1 = "000000f5a4aacc6000076360000000024200000000000000000000000000" + "000000000000000000000000000000000000"
        private const val record2 = "00000075a4aabc60000338d0000000016600000000000000000000000000" + "000000000000000000000000000000000000"
        private const val record3 = "00000075a4aaac6000000030000000016400000000000000000000000000" + "000000000000000000000000000000000000"
        private const val record4 = "00000085a4aa9c6000027750000000016200000000000000000000000000" + "000000000000000000000000000000000000"
        private const val testFile0x2 = record0 + record1 + record2 + record3 + record4
        private const val testFile0x4 = "000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000" +
                "5b88" + "000000000000000000000000000000000000000000"
        private const val testFile0xf = "0000000000b792a100"
    }
}
