/*
 * OctopusTest.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.felica.FelicaBlock
import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.felica.FelicaService
import au.id.micolous.metrodroid.card.felica.FelicaSystem
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Month
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.*

class OctopusTest : BaseInstrumentedTest() {
    private fun octopusCardFromHex(s: String, scannedAt: TimestampFull): Card {
        val data = ImmutableByteArray.fromHex(s)

        val blockBalance = FelicaBlock(data)
        val serviceBalance = FelicaService(mapOf(0 to blockBalance))

        // Don't know what the purpose of this is, but it appears empty.
        //
        // NOTE: The old card we tested doesn't respond to FeliCa discovery commands properly, so no
        // idea if that card had this service on it...
        val blockUnknown = FelicaBlock(ImmutableByteArray.empty(16))
        val serviceUnknown = FelicaService(mapOf(0 to blockUnknown))

        val system = FelicaSystem(
                mapOf(OctopusTransitData.SERVICE_OCTOPUS to serviceBalance, 0x100b to serviceUnknown))

        val f = FelicaCard(
                pMm = ImmutableByteArray.empty(8),
                systems = mapOf(OctopusTransitData.SYSTEMCODE_OCTOPUS to system))
        return Card(felica = f, scannedAt = scannedAt, tagId = ImmutableByteArray.empty(8))
    }

    private fun checkCard(c: Card, expectedBalance: TransitCurrency) {
        // Test TransitIdentity
        val i = c.parseTransitIdentity()
        assertEquals(expected=OctopusTransitData.CARD_INFO.name, actual=i!!.name)

        // Test TransitData
        val d = c.parseTransitData()
        assertTrue(message="TransitData must be instance of OctopusTransitData",
                actual=d is OctopusTransitData)

        assertEquals(expected=1, actual=d.balances!!.size)
        assertEquals(expected=expectedBalance, actual=d.balances!![0].balance)
    }

    @Test
    fun test2018Card() {
        // This data is from a card last used in 2018, but we've adjusted the date here to
        // 2017-10-02 to test the behaviour of OctopusData.getOctopusOffset.
        val c = octopusCardFromHex("00000164000000000000000000000021",
                TimestampFull(MetroTimeZone.UTC, 2017, Month.OCTOBER, 2, 0, 0))

        checkCard(c, TransitCurrency.HKD(-1440))
    }

    @Test
    fun test2016Card() {
        val c = octopusCardFromHex("000001520000000000000000000086B1",
            TimestampFull(MetroTimeZone.UTC, 2016, Month.JANUARY, 1, 0, 0))

        checkCard(c, TransitCurrency.HKD(-120))
    }
}
