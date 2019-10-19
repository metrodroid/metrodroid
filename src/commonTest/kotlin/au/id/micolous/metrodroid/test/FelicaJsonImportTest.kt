/*
 * FelicaJsonImportTest.kt
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
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.*

class FelicaJsonImportTest: CardReaderWithAssetDumpsTest(JsonKotlinFormat)  {

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

    private fun checkSkippedSystem4(felica: FelicaCard) {
        val system = felica.systems[4]
        assertNotNull(system)
        assertTrue(system.skipped)
        assertTrue(system.services.isEmpty())
    }

    /**
     * Test reading a FeliCa JSON dump with IDm tag (like <= v2.9.37)
     */
    @Test
    fun testIdm() {
        checkLoadCard("felica/felica-idm.json")
    }

    /**
     * Test reading a FeliCa JSON dump without IDm tag (like >= v2.9.38)
     */
    @Test
    fun testNoIdm() {
        checkLoadCard("felica/felica-no-idm.json")
    }

    /**
     * Test reading a FeliCa JSON dump which has a system code 4 which has an empty services map
     * and not marked as "skipped".
     */
    @Test
    fun testEmptySystem() {
        val felica = checkLoadCard("felica/felica-empty-system.json")
        val system = felica.systems[4]
        assertNotNull(system)
        assertFalse(system.skipped)
        assertTrue(system.services.isEmpty())
    }

    /**
     * Test reading a FeliCa JSON dump which has a system code 4 which has an empty services map
     * and marked as "skipped".
     */
    @Test
    fun testSkippedSystem() {
        val felica = checkLoadCard("felica/felica-skipped-system.json")
        checkSkippedSystem4(felica)
    }

    /**
     * Test reading a FeliCa JSON dump which has a system code 4 which has no services map and is
     * marked as "skipped".
     */
    @Test
    fun testSkippedSystemMissingServices() {
        val felica = checkLoadCard("felica/felica-skipped-system-missing-services.json")
        checkSkippedSystem4(felica)
    }

    /**
     * Test reading a FeliCa JSON dump which has a system code 3 with extra services:
     *
     * * 0: `"skipped": true`
     * * 1: same as `felica-idm.json`
     * * 2: `"skipped": true, "blocks": {}`
     * * 3: `"blocks": []`
     *
     * Per FeliCa spec, even-numbered service codes require authentication to access.
     */
    @Test
    fun testSkippedService() {
        val felica = checkLoadCard("felica/felica-skipped-service.json")
        val system = felica.systems[3]
        assertNotNull(system)
        for (f in listOf(0, 2, 3)) {
            val service = system.services[f]
            assertNotNull(service, "service $f must be present")
            if (f % 2 == 0) {
                assertTrue(service.skipped, "service $f must be marked as skipped")
            } else {
                assertFalse(service.skipped, "service $f must not be skipped")
            }
            assertTrue(service.blocks.isEmpty(), "service $f must have an empty list of blocks")
        }
    }

}
