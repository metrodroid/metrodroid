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

import au.id.micolous.metrodroid.card.felica.FelicaBlock
import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.felica.FelicaService
import au.id.micolous.metrodroid.card.felica.FelicaSystem
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class OctopusTest {
    private fun octopusCardFromHex(s: String, scannedAt: GregorianCalendar): FelicaCard {
        val data = ImmutableByteArray.fromHex(s)

        val blockBalance = FelicaBlock(0, data)
        val serviceBalance = FelicaService(OctopusTransitData.SERVICE_OCTOPUS, arrayOf(blockBalance))

        // Don't know what the purpose of this is, but it appears empty.
        //
        // NOTE: The old card we tested doesn't respond to FeliCa discovery commands properly, so no
        // idea if that card had this service on it...
        val blockUnknown = FelicaBlock(0, ImmutableByteArray.empty(16))
        val serviceUnknown = FelicaService(0x100b, arrayOf(blockUnknown))

        val system = FelicaSystem(OctopusTransitData.SYSTEMCODE_OCTOPUS,
                arrayOf(serviceBalance, serviceUnknown))

        return FelicaCard(ImmutableByteArray.empty(8),
                scannedAt,
                false,
                ImmutableByteArray.empty(8),
                arrayOf(system))
    }

    private fun checkCard(c: FelicaCard, expectedBalance: TransitCurrency) {
        // Test TransitIdentity
        val i = c.parseTransitIdentity()
        Assert.assertEquals(OctopusTransitData.CARD_INFO.name, i!!.name)

        // Test TransitData
        val d = c.parseTransitData()
        Assert.assertTrue("TransitData must be instance of OctopusTransitData",
                d is OctopusTransitData)

        val o = d as OctopusTransitData
        Assert.assertEquals(1, o.balances.size)
        Assert.assertEquals(expectedBalance, o.balances[0].balance)
    }

    @Test
    fun test2018Card() {
        val c = octopusCardFromHex("00000164000000000000000000000021",
                Utils.epoch(2019, null))

        checkCard(c, TransitCurrency.HKD(-1440))
    }

    @Test
    fun test2016Card() {
        val c = octopusCardFromHex("000001520000000000000000000086B1",
                Utils.epoch(2016, null))

        checkCard(c, TransitCurrency.HKD(-120))

    }
}
