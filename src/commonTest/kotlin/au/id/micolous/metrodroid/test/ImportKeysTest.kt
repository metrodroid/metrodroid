/*
 * ImportKeysTest.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.key.*
import au.id.micolous.metrodroid.key.KeyFormat
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class ImportKeysTest : BaseInstrumentedTest() {
    private fun loadTestFile(path: String): ByteArray {
        return loadSmallAssetBytes("keyTests/$path")
    }

    private fun loadTestJSON(path: String, expectedFormat: KeyFormat?): Pair<String, KeyFormat> {
        val d = loadTestFile(path)
        val f = KeyFormat.detectKeyFormat(d)
        if (expectedFormat != null) {
            assertEquals(expectedFormat, f)
        }
        return Pair(d.decodeToString(), f)
    }

    private fun loadClassicCardRawKeys(path: String): ClassicCardKeys {
        val d = loadTestFile(path)
        assertEquals(KeyFormat.RAW_MFC, KeyFormat.detectKeyFormat(d))
        return ClassicCardKeys.fromDump(d.toImmutable(), ClassicSectorKey.KeyType.A)
    }

    private fun loadClassicCardKeys(path: String, expectedID: String?, expectedFormat: KeyFormat?): ClassicKeys {
        val json = loadTestJSON(path, expectedFormat)
        val k = CardKeys.fromJSON(Json.parseToJsonElement(json.first).jsonObject,
            "test")!!

        if (expectedID != null) {
            assertEquals(expectedID, k.uid)
        }
        return k as ClassicKeys
    }

    private fun loadClassicStaticCardKeys(path: String): ClassicStaticKeys {
        val json = loadTestJSON(path, KeyFormat.JSON_MFC_STATIC)
        val k = CardKeys.fromJSON(Json.parseToJsonElement(json.first).jsonObject,
            "test")!!
        assertTrue(k is ClassicStaticKeys)
        assertEquals(CardKeys.CLASSIC_STATIC_TAG_ID, k.uid)
        return k
    }

    @Test
    fun testClassicKeys() {
        val mifare1 = loadClassicCardKeys("mifare1.json", "12345678", KeyFormat.JSON_MFC) as ClassicKeysImpl

        assertEquals(1, mifare1.getProperCandidates(0, tagId)!!.size)
        assertEquals(1, mifare1.getProperCandidates(1, tagId)!!.size)
        assertEquals(2, mifare1.getAllProperKeys(tagId).size)
        assertEquals(2, mifare1.keyCount)

        for (i in 2..15) {
            assertEquals(0, mifare1.getProperCandidates(i, tagId)?.size?:0)
        }

        val k0 = mifare1.getProperCandidates(0, tagId)!![0]
        val k1 = mifare1.getProperCandidates(1, tagId)!![0]

        assertNotNull(k0)
        assertNotNull(k1)

        assertEquals("010203040506", mifare1.getAllProperKeys(tagId)[0].key.toHexString())
        assertEquals("102030405060", mifare1.getAllProperKeys(tagId)[1].key.toHexString())

        assertEquals("010203040506", k0.key.toHexString())
        assertEquals("102030405060", k1.key.toHexString())
        assertEquals(ClassicSectorKey.KeyType.A, k0.type)
        assertEquals(ClassicSectorKey.KeyType.B, k1.type)

        // Test serialisation of ClassicCardKeys
        val j = mifare1.toJSON().toString()
        assertTrue(j.contains("KeyA"), "KeyA must be in j")
        assertTrue(j.contains("010203040506"), "010203040506 must be in j")
        assertTrue(j.contains("KeyB"), "KeyB must be in j")
        assertTrue(j.contains("102030405060"), "102030405060 must be in j")
    }

    @Test
    fun testSectorKeySerialiser() {
        val k0 = ClassicKeysImpl.classicFromJSON("{\"type\": \"KeyA\", \"key\": \"010203040506\"}", "test") as ClassicSectorKey
        val k1 = ClassicKeysImpl.classicFromJSON("{\"type\": \"KeyB\", \"key\": \"102030405060\"}", "test") as ClassicSectorKey

        assertEquals("010203040506", k0.key.toHexString())
        assertEquals("102030405060", k1.key.toHexString())
        assertEquals(ClassicSectorKey.KeyType.A, k0.type)
        assertEquals(ClassicSectorKey.KeyType.B, k1.type)

        val j0 = k0.toJSON(3).toString()
        val j1 = k1.toJSON(5).toString()

        assertTrue(j0.contains("KeyA"), "KeyA must be in j0")
        assertTrue(j0.contains("010203040506"), "010203040506 must be in j0")
        assertTrue(j1.contains("KeyB"), "KeyB must be in j1")
        assertTrue(j1.contains("102030405060"), "102030405060 must be in j1")

        val k0s = ClassicKeysImpl.classicFromJSON(j0, "test") as ClassicSectorKey
        val k1s = ClassicKeysImpl.classicFromJSON(j1, "test") as ClassicSectorKey

        val j0s = k0s.toJSON(3).toString()
        val j1s = k1s.toJSON(5).toString()

        assertEquals(j0, j0s)
        assertEquals(j1, j1s)

        assertEquals("010203040506", k0s.key.toHexString())
        assertEquals("102030405060", k1s.key.toHexString())
        assertEquals(ClassicSectorKey.KeyType.A, k0s.type)
        assertEquals(ClassicSectorKey.KeyType.B, k1s.type)
    }

    @Test
    fun testClassicStaticKeys() {
        val mifareStatic1 = loadClassicStaticCardKeys("mifareStatic1.json")

        assertEquals("Example transit agency", mifareStatic1.description)
        assertEquals(2, mifareStatic1.getProperCandidates(0, tagId)!!.size)
        assertEquals(1, mifareStatic1.getProperCandidates(10, tagId)!!.size)
        assertEquals(3, mifareStatic1.getAllProperKeys(tagId).size)

        // Shouldn't have hits on other key IDs.
        for (i in 1..9) {
            assertEquals(0, mifareStatic1.getProperCandidates(i, tagId)?.size?:0)
        }

        val k0a = mifareStatic1.getProperCandidates(0, tagId)!![0]
        val k0b = mifareStatic1.getProperCandidates(0, tagId)!![1]
        val k10 = mifareStatic1.getProperCandidates(10, tagId)!![0]

        assertNotNull(k0a)
        assertNotNull(k0b)
        assertNotNull(k10)

        assertEquals("010203040506", k0a.key.toHexString())
        assertEquals("102030405060", k0b.key.toHexString())
        assertEquals("112233445566", k10.key.toHexString())

        assertEquals(ClassicSectorKey.KeyType.A, k0a.type)
        assertEquals(ClassicSectorKey.KeyType.A, k0b.type)
        assertEquals(ClassicSectorKey.KeyType.B, k10.type)

        // Test serialisation of ClassicStaticKeys
        val j = mifareStatic1.toJSON().toString()
        assertTrue(j.contains("KeyA"), "KeyA must be in j")
        assertTrue(j.contains("010203040506"), "010203040506 must be in j")
        assertTrue(j.contains("KeyB"), "KeyB must be in j")
        assertTrue(j.contains("112233445566"), "112233445566 must be in j")
        assertTrue(j.contains("\"sector\":10"), "sector 10 must be in j")
    }

    @Test
    fun testClassicTransformKeys() {
        val keys = loadClassicStaticCardKeys("mifareTransform.json")

        assertEquals("Example transit agency", keys.description)
        assertEquals(2, keys.getProperCandidates(0, tagId)!!.size)
        assertEquals(1, keys.getProperCandidates(10, tagId)!!.size)
        assertEquals(3, keys.getAllProperKeys(tagId).size)

        // Shouldn't have hits on other key IDs.
        for (i in 1..9) {
            assertEquals(0, keys.getProperCandidates(i, tagId)?.size?:0)
        }

        val k0a = keys.getProperCandidates(0, tagId)!![0]
        val k0b = keys.getProperCandidates(0, tagId)!![1]
        val k10 = keys.getProperCandidates(10, tagId)!![0]
        val k10alt = keys.getProperCandidates(10, tagId.reverseBuffer())!![0]

        assertNotNull(k0a)
        assertNotNull(k0b)
        assertNotNull(k10)
        assertNotNull(k10alt)

        assertEquals("010203040506", k0a.key.toHexString())
        assertEquals("102030405060", k0b.key.toHexString())
        assertEquals("0b1665285566", k10.key.toHexString())
        assertEquals("617407425566", k10alt.key.toHexString())

        assertEquals(ClassicSectorKey.KeyType.A, k0a.type)
        assertEquals(ClassicSectorKey.KeyType.A, k0b.type)
        assertEquals(ClassicSectorKey.KeyType.B, k10.type)
        assertEquals(ClassicSectorKey.KeyType.B, k10alt.type)

        // Test serialisation of ClassicStaticKeys
        val j = keys.toJSON().toString()
        assertTrue(j.contains("KeyA"), "KeyA must be in j")
        assertTrue(j.contains("010203040506"), "010203040506 must be in j")
        assertTrue(j.contains("KeyB"), "KeyB must be in j")
        assertTrue(j.contains("112233445566"), "112233445566 must be in j")
        assertTrue(j.contains("\"sector\":10"), "sector 10 must be in j")
        assertTrue(j.contains("\"transform\":\"touchngo\""), "touchngo must be in j")
        assertFalse(j.contains("0b1665285566"), "kdf result must not appear in json")
        assertFalse(j.contains("617407425566"), "kdf result must not appear in json")
    }

    @Test
    fun testInvalidJSON() {
        assertFails {
            loadClassicCardKeys("invalidMifare1.json", "12345678", KeyFormat.UNKNOWN)
        }
    }

    @Test
    fun testRawKeys() {
        val k = loadClassicCardRawKeys("testkeys.farebotkeys")
        assertEquals(4, k.getAllProperKeys(tagId).size)
        for (i in 0..3) {
            assertEquals(1, k.getProperCandidates(i, tagId)!!.size)
        }

        val k0 = k.getProperCandidates(0, tagId)!![0]
        val k1 = k.getProperCandidates(1, tagId)!![0]
        val k2 = k.getProperCandidates(2, tagId)!![0]
        val k3 = k.getProperCandidates(3, tagId)!![0]

        // Null key
        assertEquals("000000000000", k0.key.toHexString())
        // Default MFC key
        assertEquals("ffffffffffff", k1.key.toHexString())
        // MIFARE Application Directory key
        assertEquals("a0a1a2a3a4a5", k2.key.toHexString())
        // NFC Forum NDEF key
        assertEquals("d3f7d3f7d3f7", k3.key.toHexString())
    }

    @Test
    fun testKeyWithBraces() {
        val k = loadClassicCardRawKeys("keyWithBraces.farebotkeys")
        assertEquals(1, k.getAllProperKeys(tagId).size)
        assertEquals(1, k.getProperCandidates(0, tagId)!!.size)

        val k0 = k.getProperCandidates(0, tagId)!![0]

        // { NULL } SPACE @ SPACE
        assertEquals("7b007d204020", k0.key.toHexString())
    }

    @Test
    fun testEmptyUID() {
        loadClassicCardKeys("mifareEmptyUID.json", null, KeyFormat.JSON_MFC_NO_UID)
    }

    @Test
    fun testNoUID() {
        loadClassicCardKeys("mifareNoUID.json", null, KeyFormat.JSON_MFC_NO_UID)
    }

    @Test
    fun testNullUID() {
        loadClassicCardKeys("mifareNullUID.json", null, KeyFormat.JSON_MFC_NO_UID)
    }

    companion object {
        val tagId = ImmutableByteArray.fromHex("12345678")
    }
}
