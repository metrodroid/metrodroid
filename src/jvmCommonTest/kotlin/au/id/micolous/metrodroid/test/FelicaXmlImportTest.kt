/*
 * FelicaXmlImportTest.kt
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

import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.serializers.XmlCardFormat
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class FelicaXmlImportTest: CardMultiReaderWithAssetDumpsTest<XmlCardFormat>(XmlCardFormat())  {
    private fun checkLoadCard(path: String): FelicaCard {
        val c = loadCard<FelicaCard>(path)
        assertEquals(ImmutableByteArray.fromHex("0101010101010101"), c.tagId)
        val felica = c.felica
        assertNotNull(felica)
        checkDummySystem3(felica)
        return felica
    }

    /**
     * Checks for the presence of a system 3 on the card, with the dummy contents: a single
     * service, 0x1, with 1 block containing 16 bytes of null.
     */
    private fun checkDummySystem3(felica: FelicaCard) {
        val system = felica.systems[3]
        assertNotNull(system)
        val service = system.services[1]
        assertNotNull(service)
        assertFalse(service.skipped)
        val blocks = service.blocks.toList()
        assertEquals(1, blocks.count())
        val data = blocks[0].data
        assertEquals(ImmutableByteArray.empty(16), data)
    }

    /**
     * Test reading a FeliCa XML dump with IDm tag (like <= v2.9.37)
     */
    @Test
    fun testFelicaXmlIdm() {
        checkLoadCard("felica/felica-idm.xml")
    }

    /**
     * Test reading a FeliCa XML dump without IDm tag (like >= v2.9.38)
     */
    @Test
    fun testFelicaXmlNoIdm() {
        checkLoadCard("felica/felica-no-idm.xml")
    }
}
