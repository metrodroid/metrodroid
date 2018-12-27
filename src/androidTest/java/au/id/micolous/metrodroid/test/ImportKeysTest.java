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
package au.id.micolous.metrodroid.test;

import android.support.annotation.Nullable;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import au.id.micolous.metrodroid.key.ClassicCardKeys;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.key.ClassicStaticKeys;
import au.id.micolous.metrodroid.util.KeyFormat;
import au.id.micolous.metrodroid.util.Utils;

import static au.id.micolous.metrodroid.key.CardKeys.CLASSIC_STATIC_TAG_ID;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class ImportKeysTest extends BaseInstrumentedTest {
    private byte[] loadTestFile(@NonNls String path) {
        return loadSmallAssetBytes("keyTests/" + path);
    }

    private Pair<JSONObject, KeyFormat> loadTestJSON(String path, @Nullable KeyFormat expectedFormat) throws JSONException {
        byte[] d = loadTestFile(path);
        KeyFormat f = Utils.detectKeyFormat(d);
        if (expectedFormat != null) {
            assertEquals(expectedFormat, f);
        }
        return new Pair<>(new JSONObject(new String(d, Utils.getASCII())), f);
    }

    private ClassicCardKeys loadClassicCardRawKeys(String path) {
        byte[] d = loadTestFile(path);
        assertEquals(KeyFormat.RAW_MFC, Utils.detectKeyFormat(d));
        return ClassicCardKeys.fromDump(d, ClassicSectorKey.KeyType.A);
    }

    private ClassicCardKeys loadClassicCardKeys(String path, @Nullable String expectedID, @Nullable KeyFormat expectedFormat) throws JSONException {
        Pair<JSONObject, KeyFormat> json = loadTestJSON(path, expectedFormat);
        ClassicCardKeys k = ClassicCardKeys.fromJSON(json.first, json.second);

        if (expectedID != null) {
            assertEquals(expectedID, k.getUID());
        }
        return k;
    }

    @SuppressWarnings("SameParameterValue")
    private ClassicStaticKeys loadClassicStaticCardKeys(String path) throws JSONException {
        Pair<JSONObject, KeyFormat> json = loadTestJSON(path, KeyFormat.JSON_MFC_STATIC);
        ClassicCardKeys k = ClassicCardKeys.fromJSON(json.first, json.second);
        assertTrue(k instanceof ClassicStaticKeys);
        assertEquals(CLASSIC_STATIC_TAG_ID, k.getUID());
        return (ClassicStaticKeys)k;
    }

    @Test
    public void testClassicKeys() throws JSONException {
        ClassicCardKeys mifare1 = loadClassicCardKeys("mifare1.json", "12345678", KeyFormat.JSON_MFC);

        assertEquals(1, mifare1.getCandidates(0).size());
        assertEquals(1, mifare1.getCandidates(1).size());
        assertEquals(2, mifare1.keys().size());

        for (int i=2; i<16; i++) {
            assertEquals(0, mifare1.getCandidates(i).size());
        }

        ClassicSectorKey k0 = mifare1.getCandidates(0).get(0);
        ClassicSectorKey k1 = mifare1.getCandidates(1).get(0);

        assertNotNull(k0);
        assertNotNull(k1);

        assertEquals(k0, mifare1.keys().get(0));
        assertEquals(k1, mifare1.keys().get(1));

        assertEquals(ClassicSectorKey.KeyType.A, k0.getType());
        assertEquals(ClassicSectorKey.KeyType.B, k1.getType());

        assertEquals("010203040506", Utils.getHexString(k0.getKey()));
        assertEquals("102030405060", Utils.getHexString(k1.getKey()));

        // Test serialisation of ClassicCardKeys
        String j = mifare1.toJSON().toString();
        assertTrue("KeyA must be in j", j.contains("KeyA"));
        assertTrue("010203040506 must be in j", j.contains("010203040506"));
        assertTrue("KeyB must be in j", j.contains("KeyB"));
        assertTrue("102030405060 must be in j", j.contains("102030405060"));
    }

    @Test
    public void testSectorKeySerialiser() throws JSONException {
        ClassicSectorKey k0 = ClassicSectorKey.fromJSON(new JSONObject("{\"type\": \"KeyA\", \"key\": \"010203040506\"}"));
        ClassicSectorKey k1 = ClassicSectorKey.fromJSON(new JSONObject("{\"type\": \"KeyB\", \"key\": \"102030405060\"}"));

        assertEquals("010203040506", Utils.getHexString(k0.getKey()));
        assertEquals("102030405060", Utils.getHexString(k1.getKey()));
        assertEquals(ClassicSectorKey.KeyType.A, k0.getType());
        assertEquals(ClassicSectorKey.KeyType.B, k1.getType());

        String j0 = k0.toJSON().toString();
        String j1 = k1.toJSON().toString();

        assertTrue("KeyA must be in j0", j0.contains("KeyA"));
        assertTrue("010203040506 must be in j0", j0.contains("010203040506"));
        assertTrue("KeyB must be in j1", j1.contains("KeyB"));
        assertTrue("102030405060 must be in j1", j1.contains("102030405060"));

        ClassicSectorKey k0s = ClassicSectorKey.fromJSON(new JSONObject(j0));
        ClassicSectorKey k1s = ClassicSectorKey.fromJSON(new JSONObject(j1));

        String j0s = k0s.toJSON().toString();
        String j1s = k1s.toJSON().toString();

        assertEquals(j0, j0s);
        assertEquals(j1, j1s);

        assertEquals("010203040506", Utils.getHexString(k0s.getKey()));
        assertEquals("102030405060", Utils.getHexString(k1s.getKey()));
        assertEquals(ClassicSectorKey.KeyType.A, k0s.getType());
        assertEquals(ClassicSectorKey.KeyType.B, k1s.getType());
    }

    @Test
    public void testClassicStaticKeys() throws JSONException {
        ClassicStaticKeys mifareStatic1 = loadClassicStaticCardKeys("mifareStatic1.json");

        assertEquals("Example transit agency", mifareStatic1.getDescription());
        assertEquals(2, mifareStatic1.getCandidates(0).size());
        assertEquals(1, mifareStatic1.getCandidates(10).size());
        assertEquals(3, mifareStatic1.keys().size());

        // Shouldn't have hits on other key IDs.
        for (int i=1; i<10; i++) {
            assertEquals(0, mifareStatic1.getCandidates(i).size());
        }

        ClassicSectorKey k0a = mifareStatic1.getCandidates(0).get(0);
        ClassicSectorKey k0b = mifareStatic1.getCandidates(0).get(1);
        ClassicSectorKey k10 = mifareStatic1.getCandidates(10).get(0);

        assertNotNull(k0a);
        assertNotNull(k0b);
        assertNotNull(k10);

        assertEquals("010203040506", Utils.getHexString(k0a.getKey()));
        assertEquals("102030405060", Utils.getHexString(k0b.getKey()));
        assertEquals("112233445566", Utils.getHexString(k10.getKey()));

        assertEquals(ClassicSectorKey.KeyType.A, k0a.getType());
        assertEquals(ClassicSectorKey.KeyType.A, k0b.getType());
        assertEquals(ClassicSectorKey.KeyType.B, k10.getType());

        // Test serialisation of ClassicStaticKeys
        String j = mifareStatic1.toJSON().toString();
        assertTrue("KeyA must be in j", j.contains("KeyA"));
        assertTrue("010203040506 must be in j", j.contains("010203040506"));
        assertTrue("KeyB must be in j", j.contains("KeyB"));
        assertTrue("112233445566 must be in j", j.contains("112233445566"));
        assertTrue("sector 10 must be in j", j.contains("\"sector\":10"));
    }

    @Test
    public void testInvalidJSON() {
        try {
            ClassicCardKeys card = loadClassicCardKeys("invalidMifare1.json", "12345678", KeyFormat.UNKNOWN);
        } catch (JSONException e) {
            assertTrue("got expected JSON throw", true);
            return;
        }

        fail("Expected JSONException");
    }

    @Test
    public void testRawKeys() {
        ClassicCardKeys k = loadClassicCardRawKeys("testkeys.farebotkeys");
        assertEquals(4, k.keys().size());
        for (int i=0; i<4; i++) {
            assertEquals(1, k.getCandidates(i).size());
        }

        ClassicSectorKey k0 = k.getCandidates(0).get(0);
        ClassicSectorKey k1 = k.getCandidates(1).get(0);
        ClassicSectorKey k2 = k.getCandidates(2).get(0);
        ClassicSectorKey k3 = k.getCandidates(3).get(0);

        // Null key
        assertEquals("000000000000", Utils.getHexString(k0.getKey()));
        // Default MFC key
        assertEquals("ffffffffffff", Utils.getHexString(k1.getKey()));
        // MIFARE Application Directory key
        assertEquals("a0a1a2a3a4a5", Utils.getHexString(k2.getKey()));
        // NFC Forum NDEF key
        assertEquals("d3f7d3f7d3f7", Utils.getHexString(k3.getKey()));
    }

    @Test
    public void testKeyWithBraces() {
        ClassicCardKeys k = loadClassicCardRawKeys("keyWithBraces.farebotkeys");
        assertEquals(1, k.keys().size());
        assertEquals(1, k.getCandidates(0).size());

        ClassicSectorKey k0 = k.getCandidates(0).get(0);

        // { NULL } SPACE @ SPACE
        assertEquals("7b007d204020", Utils.getHexString(k0.getKey()));
    }

    @Test
    public void testEmptyUID() throws Exception {
        loadClassicCardKeys("mifareEmptyUID.json", null, KeyFormat.JSON_MFC_NO_UID);
    }

    @Test
    public void testNoUID() throws Exception {
        loadClassicCardKeys("mifareNoUID.json", null, KeyFormat.JSON_MFC_NO_UID);
    }

    @Test
    public void testNullUID() throws Exception {
        loadClassicCardKeys("mifareNullUID.json", null, KeyFormat.JSON_MFC_NO_UID);
    }
}
