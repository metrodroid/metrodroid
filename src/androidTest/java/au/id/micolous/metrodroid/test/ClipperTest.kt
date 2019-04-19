/*
 * ClipperTest.java
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

import java.util.Calendar

import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.clipper.ClipperTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
/**
 * Tests for Clipper card
 */
class ClipperTest : BaseInstrumentedTest() {

    private fun constructClipperCard(): DesfireCard {
        // Construct a card to hold the data.
        val f2 = DesfireFile.create(2, null, // new RecordDesfireFileSettings((byte)0,(byte)0,null, 48, 5, 5),
                ImmutableByteArray.fromHex(testFile0x2))
        val f4 = DesfireFile.create(4, null, ImmutableByteArray.fromHex(testFile0x4))
        val f8 = DesfireFile.create(8, null,
                ImmutableByteArray.fromHex(testFile0x8))
        val fe = DesfireFile.create(0xe, null,
                ImmutableByteArray.fromHex(testFile0xe))
        val a = DesfireApplication(ClipperTransitData.APP_ID, listOf(f2, f4, f8, fe))
        return DesfireCard(ImmutableByteArray.fromHex("00010203"),
                Calendar.getInstance(), null,
                listOf(a))
    }

    @Test
    fun testDemoCard() {
        setLocale("en-US")
        showRawStationIds(false)

        assertEquals(32 * 2, refill.length)

        // This is mocked-up data, probably has a wrong checksum.
        val c = constructClipperCard()

        // Test TransitIdentity
        val i = c.parseTransitIdentity()
        assertEquals("Clipper", i!!.name)
        assertEquals("572691763", i.serialNumber)

        val d = c.parseTransitData()
        assertTrue(message = "TransitData must be instance of ClipperTransitData",
                actual = d is ClipperTransitData)

        assertEquals("572691763", d.serialNumber)
        assertEquals("Clipper", d.cardName)
        assertEquals(TransitCurrency.USD(30583), d.balance!!.balance)
        assertNull(d.subscriptions)

        val trips = d.trips
        assertNotNull(trips)
        assertEquals("Whole Foods", trips[1].getAgencyName(false))
        assertEquals("Whole Foods", trips[1].getAgencyName(true))
        assertEquals(1520009600000L, trips[1].startTimestamp!!.timeInMillis)
        assertEquals(TransitCurrency.USD(-5000), trips[1].fare)
        assertNull(trips[1].routeName)
        assertTrue(trips[1].hasTime())
        assertEquals(Trip.Mode.TICKET_MACHINE, trips[1].mode)
        assertNull(trips[1].startStation)
        assertNull(trips[1].endStation)
        assertEquals("78123456", trips[1].machineID)

        assertEquals("Bay Area Rapid Transit", trips[0].getAgencyName(false))
        assertEquals("BART", trips[0].getAgencyName(true))
        assertEquals(1521320320000L, trips[0].startTimestamp!!.timeInMillis)
        assertEquals(TransitCurrency.USD(630), trips[0].fare)
        assertNull(trips[0].routeName)
        assertTrue(trips[0].hasTime())
        assertEquals(Trip.Mode.METRO, trips[0].mode)
        assertNotNull(trips[0].startStation)
        assertEquals("Powell St.", trips[0].startStation!!.stationName)
        assertEquals("Powell St.", trips[0].startStation!!.shortStationName)
        assertNear(37.78447, java.lang.Float.parseFloat(trips[0].startStation!!.latitude).toDouble(), 0.00001)
        assertNear(-122.40797, java.lang.Float.parseFloat(trips[0].startStation!!.longitude).toDouble(), 0.00001)
        assertNotNull(trips[0].endStation)
        assertEquals("Dublin/Pleasanton", trips[0].endStation!!.stationName)
        assertEquals("Dublin/Pleasanton", trips[0].endStation!!.shortStationName)
        assertNear(37.70169, java.lang.Float.parseFloat(trips[0].endStation!!.latitude).toDouble(), 0.00001)
        assertNear(-121.89918, java.lang.Float.parseFloat(trips[0].endStation!!.longitude).toDouble(), 0.00001)
    }

    companion object {

        // mocked data
        private const val refill = "000002cfde4400007812345600001388000000000000" + "00000000000000000000"
        private const val trip = "000000040000027600000000de580000de58100000080027000000000000" + "006f"
        private const val testFile0x2 = "0000000000000000000000000000000000007777"
        private const val testFile0x4 = refill
        private const val testFile0x8 = "0022229533"
        private const val testFile0xe = trip
    }

}
