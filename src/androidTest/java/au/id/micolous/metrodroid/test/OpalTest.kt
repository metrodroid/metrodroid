/*
 * OpalTest.java
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

import android.os.Build
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.opal.OpalData
import au.id.micolous.metrodroid.transit.opal.OpalTransitData
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.ImmutableByteArray

import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Tests for Opal card
 */
class OpalTest {
    private var originalTz: TimeZone? = null

    @BeforeTest
    fun setUp() {
        originalTz = TimeZone.getDefault()
        TimeZone.setDefault(OpalTransitData.TIME_ZONE)
    }

    @AfterTest
    fun tearDown() {
        TimeZone.setDefault(originalTz)
    }

    private fun constructOpalCardFromHexString(s: String): DesfireCard {
        val demoData = ImmutableByteArray.fromHex(s)

        // Construct a card to hold the data.
        val f = DesfireFile.create(OpalTransitData.FILE_ID, null, demoData)
        val a = DesfireApplication(OpalTransitData.APP_ID, listOf(f))
        return DesfireCard(ImmutableByteArray.fromHex("00010203"),
                Calendar.getInstance(), null,
                listOf(a))
    }

    @Test
    fun testDemoCard() {
        // This is mocked-up data, probably has a wrong checksum.
        val c = constructOpalCardFromHexString("87d61200e004002a0014cc44a4133930")
        assertEquals(1, c.applications.size)
        assertNull(message = "Opal shouldn't have a valid AID",
                actual = c.getApplication(OpalTransitData.APP_ID)!!.mifareAID)

        // Test TransitIdentity
        val i = c.parseTransitIdentity()
        assertNotNull(i)
        assertEquals(OpalTransitData.NAME, i.name)
        assertEquals("3085220012345670", i.serialNumber)

        // Test TransitData
        val d = c.parseTransitData()
        assertTrue(message = "TransitData must be instance of OpalTransitData",
                actual = d is OpalTransitData)

        assertEquals("3085220012345670", d.serialNumber)
        assertEquals(TransitCurrency.AUD(336), d.balance)
        assertEquals(0, d.subscriptions!!.size)
        // 2015-10-05 09:06 UTC+11
        assertEquals("2015-10-04 22:06", Utils.isoDateTimeFormat(d.lastTransactionTime))
        assertEquals(OpalData.MODE_BUS, d.lastTransactionMode)
        assertEquals(OpalData.ACTION_JOURNEY_COMPLETED_DISTANCE, d.lastTransaction)
        assertEquals(39, d.lastTransactionNumber)
        assertEquals(1, d.weeklyTrips)
    }

    @Test
    fun testDaylightSavings() {
        // This is all mocked-up data, probably has a wrong checksum.

        // 2018-03-31 09:00 UTC+11
        // 2018-03-30 22:00 UTC
        val c1 = constructOpalCardFromHexString("85D25E07230520A70044DA380419FFFF")

        val o1 = c1.parseTransitData()
        assertTrue(message = "TransitData must be instance of OpalTransitData",
                actual = o1 is OpalTransitData)
        assertEquals("2018-03-30 22:00", Utils.isoDateTimeFormat(o1.lastTransactionTime))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals("2018-03-30T22:00Z", o1.lastTransactionTime.toInstant().atOffset(ZoneOffset.UTC).toString())
        }

        // DST transition is at 2018-04-01 03:00

        // 2018-04-01 09:00 UTC+10
        // 2018-03-31 23:00 UTC
        val c2 = constructOpalCardFromHexString("85D25E07430520A70048DA380419FFFF")

        val o2 = c2.parseTransitData()
        assertTrue(message = "TransitData must be instance of OpalTransitData",
                actual = o2 is OpalTransitData)
        assertEquals("2018-03-31 23:00", Utils.isoDateTimeFormat(o2.lastTransactionTime))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals("2018-03-31T23:00Z", o2.lastTransactionTime.toInstant().atOffset(ZoneOffset.UTC).toString())
        }
    }

}
