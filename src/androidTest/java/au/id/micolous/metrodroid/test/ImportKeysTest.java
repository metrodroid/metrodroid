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

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.Nullable;
import android.test.InstrumentationTestCase;
import android.util.Pair;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import au.id.micolous.metrodroid.key.ClassicCardKeys;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.key.ClassicStaticKeys;
import au.id.micolous.metrodroid.util.KeyFormat;
import au.id.micolous.metrodroid.util.Utils;

import static au.id.micolous.metrodroid.key.CardKeys.CLASSIC_STATIC_TAG_ID;

public class ImportKeysTest extends InstrumentationTestCase {
    private byte[] loadTestFile(String path) throws IOException {
        Context ctx = getInstrumentation().getContext();
        InputStream i = ctx.getAssets().open("keyTests/" + path, AssetManager.ACCESS_RANDOM);
        int length = i.available();
        if (length > 10240 || length <= 0) {
            throw new IOException("Expected 0 - 10240 bytes");
        }

        byte[] out = new byte[length];
        int realLen = i.read(out);

        // Return truncated buffer
        return ArrayUtils.subarray(out, 0, realLen);
    }

    private Pair<JSONObject, KeyFormat> loadTestJSON(String path, @Nullable KeyFormat expectedFormat) throws IOException, JSONException {
        byte[] d = loadTestFile(path);
        KeyFormat f = Utils.detectKeyFormat(d);
        if (expectedFormat != null) {
            assertEquals(expectedFormat, f);
        }
        return new Pair<>(new JSONObject(new String(d)), f);
    }

    private ClassicCardKeys loadClassicCardRawKeys(String path) throws IOException {
        byte[] d = loadTestFile(path);
        assertEquals(KeyFormat.RAW_MFC, Utils.detectKeyFormat(d));
        return ClassicCardKeys.fromDump(d, ClassicSectorKey.KeyType.A);
    }

    private ClassicCardKeys loadClassicCardKeys(String path, @Nullable String expectedID, @Nullable KeyFormat expectedFormat) throws IOException, JSONException {
        Pair<JSONObject, KeyFormat> json = loadTestJSON(path, expectedFormat);
        ClassicCardKeys k = ClassicCardKeys.fromJSON(json.first, json.second);

        if (expectedID != null) {
            assertEquals(expectedID, k.getUID());
        }
        return k;
    }

    private ClassicStaticKeys loadClassicStaticCardKeys(String path) throws IOException, JSONException {
        Pair<JSONObject, KeyFormat> json = loadTestJSON(path, KeyFormat.JSON_MFC_STATIC);
        ClassicCardKeys k = ClassicCardKeys.fromJSON(json.first, json.second);
        assertTrue(k instanceof ClassicStaticKeys);
        assertEquals(CLASSIC_STATIC_TAG_ID, k.getUID());
        return (ClassicStaticKeys)k;
    }

    public void testClassicKeys() throws IOException, JSONException {
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
    }

    public void testClassicStaticKeys() throws IOException, JSONException {
        ClassicStaticKeys mifareStatic1 = loadClassicStaticCardKeys("mifareStatic1.json");

        assertEquals("Example transit agency", mifareStatic1.getDescription());
        assertEquals(2, mifareStatic1.getCandidates(0).size());
        assertEquals(1, mifareStatic1.getCandidates(10).size());
        assertEquals(3, mifareStatic1.keys().size());

        // Shouldn't have hits on other key IDs.
        for (int i=1; i<10; i++) {
            assertEquals(0, mifareStatic1.getCandidates(i).size());
        }

        // All keys are KeyA.
        for (ClassicSectorKey k : mifareStatic1.keys()) {
            assertEquals(ClassicSectorKey.KeyType.A, k.getType());
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
    }

    public void testInvalidJSON() throws IOException {
        try {
            ClassicCardKeys card = loadClassicCardKeys("invalidMifare1.json", "12345678", KeyFormat.UNKNOWN);
        } catch (JSONException e) {
            assertTrue("got expected JSON throw", true);
            return;
        }

        assertTrue("Expected JSONException", false);
    }

    public void testRawKeys() throws IOException {
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

    public void testKeyWithBraces() throws IOException {
        ClassicCardKeys k = loadClassicCardRawKeys("keyWithBraces.farebotkeys");
        assertEquals(1, k.keys().size());
        assertEquals(1, k.getCandidates(0).size());

        ClassicSectorKey k0 = k.getCandidates(0).get(0);

        // { NULL } SPACE @ SPACE
        assertEquals("7b007d204020", Utils.getHexString(k0.getKey()));
    }

    public void testEmptyUID() throws Exception {
        loadClassicCardKeys("mifareEmptyUID.json", null, KeyFormat.JSON_MFC_NO_UID);
    }

    public void testNoUID() throws Exception {
        loadClassicCardKeys("mifareNoUID.json", null, KeyFormat.JSON_MFC_NO_UID);
    }

    public void testNullUID() throws Exception {
        loadClassicCardKeys("mifareNullUID.json", null, KeyFormat.JSON_MFC_NO_UID);
    }
}
