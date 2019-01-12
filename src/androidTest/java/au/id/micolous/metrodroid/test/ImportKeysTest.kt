/*
 * ImportKeysTest.java
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

import android.util.Pair
import au.id.micolous.metrodroid.key.*
import au.id.micolous.metrodroid.util.KeyFormat
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.xml.toImmutable
import junit.framework.TestCase. fail
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import org.jetbrains.annotations.NonNls
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class ImportKeysTest : BaseInstrumentedTest() {
    private fun loadTestFile(@NonNls path: String): ByteArray {
        return loadSmallAssetBytes("keyTests/$path")
    }

    @Throws(JSONException::class)
    private fun loadTestJSON(path: String, expectedFormat: KeyFormat?): Pair<JSONObject, KeyFormat> {
        val d = loadTestFile(path)
        val f = Utils.detectKeyFormat(d)
        if (expectedFormat != null) {
            TestCase.assertEquals(expectedFormat, f)
        }
        return Pair(JSONObject(String(d, Utils.getASCII())), f)
    }

    private fun loadClassicCardRawKeys(path: String): ClassicCardKeys {
        val d = loadTestFile(path)
        TestCase.assertEquals(KeyFormat.RAW_MFC, Utils.detectKeyFormat(d))
        return ClassicCardKeys.fromDump(d.toImmutable(), ClassicSectorKey.KeyType.A)
    }

    @Throws(JSONException::class)
    private fun loadClassicCardKeys(path: String, expectedID: String?, expectedFormat: KeyFormat?): ClassicKeys {
        val json = loadTestJSON(path, expectedFormat)
        val k = CardKeys.fromJSON(json.first, "test")!!

        if (expectedID != null) {
            assertEquals(expectedID, k.uid)
        }
        return k as ClassicKeys
    }

    @Throws(JSONException::class)
    private fun loadClassicStaticCardKeys(path: String): ClassicStaticKeys {
        val json = loadTestJSON(path, KeyFormat.JSON_MFC_STATIC)
        val k = CardKeys.fromJSON(json.first, "test")!!
        TestCase.assertTrue(k is ClassicStaticKeys)
        assertEquals(CardKeys.CLASSIC_STATIC_TAG_ID, k.uid)
        return k as ClassicStaticKeys
    }

    @Test
    @Throws(JSONException::class)
    fun testClassicKeys() {
        val mifare1 = loadClassicCardKeys("mifare1.json", "12345678", KeyFormat.JSON_MFC) as ClassicKeysImpl

        TestCase. assertEquals(1, mifare1.getProperCandidates(0)!!.size)
        TestCase. assertEquals(1, mifare1.getProperCandidates(1)!!.size)
        assertEquals(2, mifare1.allProperKeys.size)
        assertEquals(2, mifare1.keyCount)

        for (i in 2..15) {
            TestCase. assertEquals(0, mifare1.getProperCandidates(i)?.size?:0)
        }

        val k0 = mifare1.getProperCandidates(0)!![0]
        val k1 = mifare1.getProperCandidates(1)!![0]

        TestCase. assertNotNull(k0)
        TestCase. assertNotNull(k1)

        TestCase. assertEquals("010203040506", mifare1.allProperKeys[0].key.toHexString())
        TestCase. assertEquals("102030405060", mifare1.allProperKeys[1].key.toHexString())

        TestCase. assertEquals("010203040506", k0.key.toHexString())
        TestCase. assertEquals("102030405060", k1.key.toHexString())
        TestCase. assertEquals(ClassicSectorKey.KeyType.A, k0.type)
        TestCase. assertEquals(ClassicSectorKey.KeyType.B, k1.type)

        // Test serialisation of ClassicCardKeys
        val j = mifare1.toJSON().toString()
        TestCase. assertTrue("KeyA must be in j", j.contains("KeyA"))
        TestCase. assertTrue("010203040506 must be in j", j.contains("010203040506"))
        TestCase. assertTrue("KeyB must be in j", j.contains("KeyB"))
        TestCase. assertTrue("102030405060 must be in j", j.contains("102030405060"))
    }

    @Test
    @Throws(JSONException::class)
    fun testSectorKeySerialiser() {
        val k0 = ClassicSectorKey.fromJSON(JSONObject("{\"type\": \"KeyA\", \"key\": \"010203040506\"}"), "test")
        val k1 = ClassicSectorKey.fromJSON(JSONObject("{\"type\": \"KeyB\", \"key\": \"102030405060\"}"), "test")

        TestCase. assertEquals("010203040506", k0.key.toHexString())
        TestCase. assertEquals("102030405060", k1.key.toHexString())
        TestCase. assertEquals(ClassicSectorKey.KeyType.A, k0.type)
        TestCase. assertEquals(ClassicSectorKey.KeyType.B, k1.type)

        val j0 = k0.toJSON().toString()
        val j1 = k1.toJSON().toString()

        TestCase. assertTrue("KeyA must be in j0", j0.contains("KeyA"))
        TestCase. assertTrue("010203040506 must be in j0", j0.contains("010203040506"))
        TestCase. assertTrue("KeyB must be in j1", j1.contains("KeyB"))
        TestCase. assertTrue("102030405060 must be in j1", j1.contains("102030405060"))

        val k0s = ClassicSectorKey.fromJSON(JSONObject(j0), "test")
        val k1s = ClassicSectorKey.fromJSON(JSONObject(j1), "test")

        val j0s = k0s.toJSON().toString()
        val j1s = k1s.toJSON().toString()

        TestCase. assertEquals(j0, j0s)
        TestCase. assertEquals(j1, j1s)

        TestCase. assertEquals("010203040506", k0s.key.toHexString())
        TestCase. assertEquals("102030405060", k1s.key.toHexString())
        TestCase. assertEquals(ClassicSectorKey.KeyType.A, k0s.type)
        TestCase. assertEquals(ClassicSectorKey.KeyType.B, k1s.type)
    }

    @Test
    @Throws(JSONException::class)
    fun testClassicStaticKeys() {
        val mifareStatic1 = loadClassicStaticCardKeys("mifareStatic1.json")

        TestCase. assertEquals("Example transit agency", mifareStatic1.description)
        TestCase. assertEquals(2, mifareStatic1.getProperCandidates(0)!!.size)
        TestCase. assertEquals(1, mifareStatic1.getProperCandidates(10)!!.size)
        assertEquals(3, mifareStatic1.allProperKeys.size)

        // Shouldn't have hits on other key IDs.
        for (i in 1..9) {
            TestCase. assertEquals(0, mifareStatic1.getProperCandidates(i)?.size?:0)
        }

        val k0a = mifareStatic1.getProperCandidates(0)!![0]
        val k0b = mifareStatic1.getProperCandidates(0)!![1]
        val k10 = mifareStatic1.getProperCandidates(10)!![0]

        TestCase. assertNotNull(k0a)
        TestCase. assertNotNull(k0b)
        TestCase. assertNotNull(k10)

        TestCase. assertEquals("010203040506", k0a.key.toHexString())
        TestCase. assertEquals("102030405060", k0b.key.toHexString())
        TestCase. assertEquals("112233445566", k10.key.toHexString())

        TestCase. assertEquals(ClassicSectorKey.KeyType.A, k0a.type)
        TestCase. assertEquals(ClassicSectorKey.KeyType.A, k0b.type)
        TestCase. assertEquals(ClassicSectorKey.KeyType.B, k10.type)

        // Test serialisation of ClassicStaticKeys
        val j = mifareStatic1.toJSON().toString()
        TestCase. assertTrue("KeyA must be in j", j.contains("KeyA"))
        TestCase. assertTrue("010203040506 must be in j", j.contains("010203040506"))
        TestCase. assertTrue("KeyB must be in j", j.contains("KeyB"))
        TestCase. assertTrue("112233445566 must be in j", j.contains("112233445566"))
        TestCase. assertTrue("sector 10 must be in j", j.contains("\"sector\":10"))
    }

    @Test
    fun testInvalidJSON() {
        try {
            val card = loadClassicCardKeys("invalidMifare1.json", "12345678", KeyFormat.UNKNOWN)
        } catch (e: JSONException) {
            TestCase. assertTrue("got expected JSON throw", true)
            return
        }

        fail("Expected JSONException")
    }

    @Test
    fun testRawKeys() {
        val k = loadClassicCardRawKeys("testkeys.farebotkeys")
        assertEquals(4, k.allProperKeys.size)
        for (i in 0..3) {
            TestCase. assertEquals(1, k.getProperCandidates(i)!!.size)
        }

        val k0 = k.getProperCandidates(0)!![0]
        val k1 = k.getProperCandidates(1)!![0]
        val k2 = k.getProperCandidates(2)!![0]
        val k3 = k.getProperCandidates(3)!![0]

        // Null key
        TestCase. assertEquals("000000000000", k0.key.toHexString())
        // Default MFC key
        TestCase. assertEquals("ffffffffffff", k1.key.toHexString())
        // MIFARE Application Directory key
        TestCase. assertEquals("a0a1a2a3a4a5", k2.key.toHexString())
        // NFC Forum NDEF key
        TestCase. assertEquals("d3f7d3f7d3f7", k3.key.toHexString())
    }

    @Test
    fun testKeyWithBraces() {
        val k = loadClassicCardRawKeys("keyWithBraces.farebotkeys")
        assertEquals(1, k.allProperKeys.size)
        TestCase. assertEquals(1, k.getProperCandidates(0)!!.size)

        val k0 = k.getProperCandidates(0)!![0]

        // { NULL } SPACE @ SPACE
        TestCase. assertEquals("7b007d204020", k0.key.toHexString())
    }

    @Test
    @Throws(Exception::class)
    fun testEmptyUID() {
        loadClassicCardKeys("mifareEmptyUID.json", null, KeyFormat.JSON_MFC_NO_UID)
    }

    @Test
    @Throws(Exception::class)
    fun testNoUID() {
        loadClassicCardKeys("mifareNoUID.json", null, KeyFormat.JSON_MFC_NO_UID)
    }

    @Test
    @Throws(Exception::class)
    fun testNullUID() {
        loadClassicCardKeys("mifareNullUID.json", null, KeyFormat.JSON_MFC_NO_UID)
    }
}
