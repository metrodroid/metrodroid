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
import kotlin.test.assertNotNull

class FelicaXmlImportTest: CardReaderWithAssetDumpsTest(XmlCardFormat())  {
    /**
     * Test reading a FeliCa XML dump with IDm tag (like <= v2.9.37)
     */
    @Test
    fun testFelicaXmlIdm() {
        val c = loadCard<FelicaCard>("felica/felica-idm.xml")
        assertEquals(ImmutableByteArray.fromHex("0101010101010101"), c.tagId)
        val felica = c.felica
        assertNotNull(felica)
        assertEquals(setOf(3), felica.systems.keys)
    }

    /**
     * Test reading a FeliCa XML dump without IDm tag (like >= v2.9.38)
     */
    @Test
    fun testFelicaXmlNoIdm() {
        val c = loadCard<FelicaCard>("felica/felica-no-idm.xml")
        assertEquals(ImmutableByteArray.fromHex("0101010101010101"), c.tagId)
        val felica = c.felica
        assertNotNull(felica)
        assertEquals(setOf(3), felica.systems.keys)
    }
}