/*
 * ClipperTest.kt
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
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.clipper.ClipperTransitData
import au.id.micolous.metrodroid.transit.clipper.ClipperTrip
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.*


/**
 * Tests for Clipper card
 */
class ClipperTest : BaseInstrumentedTest() {

    private fun constructClipperCard(): DesfireCard {
        // Construct a card to hold the data.
        val f2 = DesfireFile.create(ImmutableByteArray.fromHex("00000000000000"), // new RecordDesfireFileSettings((byte)0,(byte)0,null, 48, 5, 5),
                ImmutableByteArray.fromHex(testFile0x2))
        val f4 = DesfireFile.create(ImmutableByteArray.fromHex("00000000000000"),
                ImmutableByteArray.fromHex(testFile0x4))
        val f8 = DesfireFile.create(ImmutableByteArray.fromHex("00000000000000"),
                ImmutableByteArray.fromHex(testFile0x8))
        val fe = DesfireFile.create(ImmutableByteArray.fromHex("00000000000000"),
                ImmutableByteArray.fromHex(testFile0xe))
        val a = DesfireApplication(mapOf(2 to f2.raw, 4 to f4.raw, 8 to f8.raw, 0xe to fe.raw),
                authLog = emptyList())
        return DesfireCard(
                ImmutableByteArray.empty(),
                mapOf(ClipperTransitData.APP_ID to a))
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
        assertTrue(d is ClipperTransitData, "TransitData must be instance of ClipperTransitData")

        val o = d as ClipperTransitData?
        assertEquals("572691763", o!!.serialNumber)
        assertEquals("Clipper", o.cardName)
        assertEquals(TransitCurrency.USD(30583), o.balance!!.balance)
        assertNull(o.subscriptions)

        val trips = o.trips
        assertNotNull(trips)
        assertEquals("Whole Foods", trips[1].getAgencyName(false)?.unformatted)
        assertEquals("Whole Foods", trips[1].getAgencyName(true)?.unformatted)
        assertEquals(1520009600000L, (trips[1].startTimestamp as TimestampFull).timeInMillis)
        assertEquals(TransitCurrency.USD(-5000), trips[1].fare)
        assertNull(trips[1].routeName)
        assertEquals(Trip.Mode.TICKET_MACHINE, trips[1].mode)
        assertNull(trips[1].startStation)
        assertNull(trips[1].endStation)
        assertEquals("78123456", trips[1].machineID)

        assertEquals("Bay Area Rapid Transit", trips[0].getAgencyName(false)?.unformatted)
        assertEquals("BART", trips[0].getAgencyName(true)?.unformatted)
        assertEquals(1521320320000L, (trips[0].startTimestamp as TimestampFull).timeInMillis)
        assertEquals(TransitCurrency.USD(630), trips[0].fare)
        assertNull(trips[0].routeName)
        assertEquals(Trip.Mode.METRO, trips[0].mode)
        assertNotNull(trips[0].startStation)
        assertEquals("Powell Street", trips[0].startStation!!.getStationName(false).unformatted)
        assertEquals("Powell Street", trips[0].startStation!!.getStationName(true).unformatted)
        assertNear(37.78447, trips[0].startStation!!.latitude!!.toDouble(), 0.001)
        assertNear(-122.40797, trips[0].startStation!!.longitude!!.toDouble(), 0.001)
        assertNotNull(trips[0].endStation)
        assertEquals("Dublin / Pleasanton", trips[0].endStation!!.getStationName(false).unformatted)
        assertEquals("Dublin / Pleasanton", trips[0].endStation!!.getStationName(true).unformatted)
        assertNear(37.70169, trips[0].endStation!!.latitude!!.toDouble(), 0.001)
        assertNear(-121.89918, trips[0].endStation!!.longitude!!.toDouble(), 0.001)
    }

    @Test
    fun testVehicleNumbers() {
        assertEquals(null, ClipperTrip(ImmutableByteArray.fromHex("0000000000000000000000000000000000000000000000000000000000000000")).vehicleID)
        assertEquals(null, ClipperTrip(ImmutableByteArray.fromHex("00000000000000000000ffff0000000000000000000000000000000000000000")).vehicleID)
        // These trips below were extracted from an actual clipper card
        assertEquals("1058", ClipperTrip(ImmutableByteArray.fromHex("10000012000000fa008a0422e1a11e84000000000000ffffff00000000010061")).vehicleID)
        assertEquals("2010A", ClipperTrip(ImmutableByteArray.fromHex("10000012000000fa008a4e85e1a0ff9a000000000000ffff0000000000010075")).vehicleID)
        assertEquals("2061B", ClipperTrip(ImmutableByteArray.fromHex("10000012000000fa008a5084e19ce9c1000000000000ffff0000000000010075")).vehicleID)
        assertEquals("1525", ClipperTrip(ImmutableByteArray.fromHex("10000012000000fa008a05f5e1a01426000000000000ffffff00000000010061")).vehicleID)
    }

    companion object {

        // mocked data
        private const val refill = "000002cfde440000781234560000138800000000000000000000000000000000"
        private const val trip = "000000040000027600000000de580000de58100000080027000000000000006f"
        private const val testFile0x2 = "0000000000000000000000000000000000007777"
        private const val testFile0x4 = refill
        private const val testFile0x8 = "0022229533"
        private const val testFile0xe = trip
    }

}
