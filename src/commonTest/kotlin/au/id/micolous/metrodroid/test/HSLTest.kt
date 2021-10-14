/*
 * HSLTest.kt
 *
 * Copyright 2019 Google
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
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Month
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.hsl.HSLArvo
import au.id.micolous.metrodroid.transit.hsl.HSLKausi
import au.id.micolous.metrodroid.transit.hsl.HSLTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Preferences
import kotlin.test.*

/**
 * Tests for HSL card
 */

class HSLTest : BaseInstrumentedTest() {

    private fun constructHSLCardV2(): DesfireCard {
        // Construct a card to hold the data.
        // Based on my card but with sensitive data altered
        val f0 = DesfireFile.create(ImmutableByteArray.fromHex("010110e10a0000"),
                ImmutableByteArray.fromHex("00000000000300000000"))
        // Kausi: entirely fake
        val f1 = DesfireFile.create(ImmutableByteArray.fromHex("010110e1230000"),
                ImmutableByteArray.fromHex("01ff15001404000000000000000001ff000af20f02710006e04d000000000000000000"))
        val f2 = DesfireFile.create(ImmutableByteArray.fromHex("010110e10d0000"),
                ImmutableByteArray.fromHex("000287ffec1800fa0000001000"))
        // Arvo
        val f3 = DesfireFile.create(ImmutableByteArray.fromHex("010110e12d0000"),
                ImmutableByteArray.fromHex("81f40000410b400413fff7000203980000200000000000000003fff65e0000a200000005fffb2e21945dd20800"))
        val f4 = DesfireFile.create(ImmutableByteArray.fromHex("040110e10c0000080000070000"),
                ImmutableByteArray.fromHex("000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                        "000000000000000000000000000000000000000000000000000000bfff65e0000a20e602000500"))
        val f5 = DesfireFile.create(ImmutableByteArray.fromHex("010010ed0c0000"),
                ImmutableByteArray.fromHex("000000000000000000000000"))
        val f8 = DesfireFile.create(ImmutableByteArray.fromHex("000100e00b0000"),
                ImmutableByteArray.fromHex("2192462000112345678910"))
        // Files 9 and 10 are unauthorized

        val a = DesfireApplication(mapOf(
                0 to f0.raw,
                1 to f1.raw,
                2 to f2.raw,
                3 to f3.raw,
                4 to f4.raw,
                5 to f5.raw,
                8 to f8.raw
                ), emptyList())
        return DesfireCard(ImmutableByteArray.empty(),
                mapOf(HSLTransitData.APP_ID_V2 to a))
    }

    @Test
    fun testDemoCard() {
        setLocale("en-US")
        Preferences.showRawStationIds = false

        // This is mocked-up data, probably has a wrong checksum.
        val c = constructHSLCardV2()

        // Test TransitIdentity
        val i = c.parseTransitIdentity()
        assertEquals("HSL", i!!.name)
        assertEquals("924620 0011 2345 6789", i.serialNumber)

        val d = c.parseTransitData()
        assertTrue(d is HSLTransitData, "TransitData must be instance of HSLTransitData")

        val o = d as HSLTransitData?
        assertEquals("924620 0011 2345 6789", o!!.serialNumber)
        assertEquals("HSL", o.cardName)
        assertEquals(TransitCurrency.EUR(40), o.balance)

        val subs = o.subscriptions!!
        assertEquals(2, subs.size)

        val periodPass = subs[0] as HSLKausi
        assertEquals("PeriodPass for zones BC", periodPass.subscriptionName)
        assertEquals(77, periodPass.machineId)
        assertEquals(TimestampFull(MetroTimeZone.HELSINKI, 2019, Month.JUNE, 8,12,34), periodPass.purchaseTimestamp)
        assertEquals(TransitCurrency.EUR(20000), periodPass.cost)
        assertEquals("30 calendar days", periodPass.formatPeriod())
        assertEquals(Daystamp(2019, Month.JUNE, 9), periodPass.validFrom)
        assertEquals(Daystamp(2019, Month.JULY, 9), periodPass.validTo)
        assertEquals(1, periodPass.passengerCount)

        val eticket = subs[1] as HSLArvo
        assertEquals("eTicket for zones ABC", eticket.subscriptionName)
        assertEquals(TransitCurrency.EUR(460), eticket.cost)
        assertEquals("90 minutes", eticket.formatPeriod())
        assertEquals("Adult", eticket.profile)
        assertEquals("English", eticket.language)
        assertEquals(Daystamp(2019, Month.JUNE, 6), eticket.purchaseTimestamp) // FIXME: check hour

        assertEquals(TimestampFull(MetroTimeZone.HELSINKI, 2019, Month.JUNE, 6,23,51), eticket.validFrom)
        assertEquals(TimestampFull(MetroTimeZone.HELSINKI, 2019, Month.JUNE, 7,1,21), eticket.validTo)
        assertEquals(1, eticket.passengerCount)

        val trips = o.trips.sortedWith(Trip.Comparator())
        assertNotNull(trips)
        assertEquals(2, trips.size)

        assertEquals("Value ticket, 90 mins", trips[0].getAgencyName(false)!!.unformatted)
        assertEquals("Value ticket, 90 mins", trips[0].getAgencyName(true)!!.unformatted)
        assertEquals(1559854260L * 1000, (trips[0].startTimestamp as TimestampFull).timeInMillis)
        assertEquals(TransitCurrency.EUR(460), trips[0].fare)
        assertEquals("2", trips[0].routeName!!.unformatted)
        assertEquals(Trip.Mode.TRAIN, trips[0].mode)
        assertEquals("Zone C", trips[0].startStation!!.stationName!!.unformatted)
        assertNull(trips[0].endStation)
        assertEquals("1074", trips[0].vehicleID)

        assertEquals("Balance refill", trips[1].getAgencyName(false)!!.unformatted)
        assertEquals("Balance refill", trips[1].getAgencyName(true)!!.unformatted)
        assertEquals(1559853060L * 1000, (trips[1].startTimestamp as TimestampFull).timeInMillis)
        assertEquals(TransitCurrency.EUR(-500), trips[1].fare)
        assertNull(trips[1].routeName)
        assertEquals(Trip.Mode.TICKET_MACHINE, trips[1].mode)
        assertNull(trips[1].startStation)
        assertNull(trips[1].endStation)
    }
}
