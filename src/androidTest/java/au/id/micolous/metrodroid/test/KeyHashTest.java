/*
 * KeyHashTest.java
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

import static junit.framework.TestCase.assertEquals;

/**
 * This test validates {@link Utils#checkKeyHash(ImmutableByteArray, String, String...)} such that:
 *
 * 1. The KeyHash algorithm hasn't changed.
 *
 * 2. The arguments are working in an expected way.
 *
 * Please do not change this implementation, as this will break other card readers that depend on
 * it. This test is intended to make it easy
 */
@RunWith(JUnit4.class)
public class KeyHashTest {
    private static final ImmutableByteArray MAD_KEY = ImmutableByteArray.Companion.fromHex("A0A1A2A3A4A5");
    private static final ImmutableByteArray DEFAULT_KEY = ImmutableByteArray.Companion.fromHex("FFFFFFFFFFFF");
    private static final ClassicSectorKey MAD_SECTOR_KEY =
            ClassicSectorKey.Companion.fromDump(MAD_KEY,
            ClassicSectorKey.KeyType.A, "test");
    private static final ClassicSectorKey DEFAULT_SECTOR_KEY =
            ClassicSectorKey.Companion.fromDump(DEFAULT_KEY,
                    ClassicSectorKey.KeyType.A, "test");

    private static final String SALT0 = "sodium chloride";
    private static final String MAD_HASH0 = "fc18681fd880307349238c72268aae3b";
    private static final String DEFAULT_HASH0 = "1a0aea4daffab36129fc4a760567a823";

    private static final String SALT1 = "bath";
    private static final String MAD_HASH1 = "93bf0db4fc97682b9d79dc667c046b88";
    private static final String DEFAULT_HASH1 = "878156605169c3070573998b35e08846";

    private static final String SALT2 = "cracked pepper";
    private static final String MAD_HASH2 = "42451d2b7c8338b7d4f60313c5f4e3f3";
    private static final String DEFAULT_HASH2 = "dfc7fcfdcff15daf0b71226cbf87cb32";

    @Test
    public void testIncorrectKeyHash() {
        // Test with just 1 possible answer
        assertEquals(-1, Utils.checkKeyHash(MAD_KEY, SALT0, MAD_HASH1));

        // Then test with multiple
        assertEquals(-1, Utils.checkKeyHash(MAD_KEY, SALT0,
                MAD_HASH1, MAD_HASH2,
                DEFAULT_HASH0, DEFAULT_HASH1, DEFAULT_HASH2));
        assertEquals(-1, Utils.checkKeyHash(MAD_KEY, SALT1,
                MAD_HASH0, MAD_HASH2,
                DEFAULT_HASH0, DEFAULT_HASH1, DEFAULT_HASH2));
        assertEquals(-1, Utils.checkKeyHash(MAD_KEY, SALT2,
                MAD_HASH0, MAD_HASH1,
                DEFAULT_HASH0, DEFAULT_HASH1, DEFAULT_HASH2));

        assertEquals(-1, Utils.checkKeyHash(DEFAULT_KEY, SALT0,
                MAD_HASH0, MAD_HASH1, MAD_HASH2,
                DEFAULT_HASH1, DEFAULT_HASH2));
        assertEquals(-1, Utils.checkKeyHash(DEFAULT_KEY, SALT1,
                MAD_HASH0, MAD_HASH1, MAD_HASH2,
                DEFAULT_HASH0, DEFAULT_HASH2));
        assertEquals(-1, Utils.checkKeyHash(DEFAULT_KEY, SALT2,
                MAD_HASH0, MAD_HASH1, MAD_HASH2,
                DEFAULT_HASH0, DEFAULT_HASH1));
    }

    @Test
    public void test1CorrectKeyHash() {
        // Checking when there is one right answer.
        // This is to validate that the algorithm is giving diverse-enough results.
        assertEquals(0, Utils.checkKeyHash(MAD_KEY, SALT0, MAD_HASH0));
        assertEquals(0, Utils.checkKeyHash(MAD_KEY, SALT1, MAD_HASH1));
        assertEquals(0, Utils.checkKeyHash(MAD_KEY, SALT2, MAD_HASH2));

        assertEquals(0, Utils.checkKeyHash(DEFAULT_KEY, SALT0, DEFAULT_HASH0));
        assertEquals(0, Utils.checkKeyHash(DEFAULT_KEY, SALT1, DEFAULT_HASH1));
        assertEquals(0, Utils.checkKeyHash(DEFAULT_KEY, SALT2, DEFAULT_HASH2));
    }

    @Test
    public void testOffsetCorrectKeyHash() {
        assertEquals(1, Utils.checkKeyHash(MAD_KEY, SALT1,
                MAD_HASH0, MAD_HASH1));
        assertEquals(1, Utils.checkKeyHash(MAD_KEY, SALT1,
                MAD_HASH0, MAD_HASH1, MAD_HASH2));

        assertEquals(2, Utils.checkKeyHash(MAD_KEY, SALT2,
                MAD_HASH0, MAD_HASH1, MAD_HASH2));
    }

    @Test
    public void testRepeatedCorrectKeyHash() {
        assertEquals(0, Utils.checkKeyHash(DEFAULT_KEY, SALT0,
                DEFAULT_HASH0, DEFAULT_HASH0, DEFAULT_HASH1));
        assertEquals(0, Utils.checkKeyHash(DEFAULT_KEY, SALT0,
                DEFAULT_HASH0, DEFAULT_HASH1, DEFAULT_HASH0));

        assertEquals(2, Utils.checkKeyHash(DEFAULT_KEY, SALT1,
                DEFAULT_HASH0, DEFAULT_HASH0, DEFAULT_HASH1));
        assertEquals(2, Utils.checkKeyHash(DEFAULT_KEY, SALT1,
                DEFAULT_HASH0, DEFAULT_HASH0, DEFAULT_HASH1, DEFAULT_HASH1));

    }

    @Test
    public void testWrappedKeyHash() {
        assertEquals(0, Utils.checkKeyHash(MAD_SECTOR_KEY, SALT0, MAD_HASH0));
        assertEquals(0, Utils.checkKeyHash(MAD_SECTOR_KEY, SALT1, MAD_HASH1));
        assertEquals(0, Utils.checkKeyHash(MAD_SECTOR_KEY, SALT2, MAD_HASH2));

        assertEquals(0, Utils.checkKeyHash(DEFAULT_SECTOR_KEY, SALT0, DEFAULT_HASH0));
        assertEquals(0, Utils.checkKeyHash(DEFAULT_SECTOR_KEY, SALT1, DEFAULT_HASH1));
        assertEquals(0, Utils.checkKeyHash(DEFAULT_SECTOR_KEY, SALT2, DEFAULT_HASH2));

        assertEquals(-1, Utils.checkKeyHash(MAD_SECTOR_KEY, SALT0,
                MAD_HASH1, MAD_HASH2,
                DEFAULT_HASH0, DEFAULT_HASH1, DEFAULT_HASH2));
        assertEquals(-1, Utils.checkKeyHash(MAD_SECTOR_KEY, SALT1,
                MAD_HASH0, MAD_HASH2,
                DEFAULT_HASH0, DEFAULT_HASH1, DEFAULT_HASH2));
        assertEquals(-1, Utils.checkKeyHash(MAD_SECTOR_KEY, SALT2,
                MAD_HASH0, MAD_HASH1,
                DEFAULT_HASH0, DEFAULT_HASH1, DEFAULT_HASH2));

        assertEquals(-1, Utils.checkKeyHash(DEFAULT_SECTOR_KEY, SALT0,
                MAD_HASH0, MAD_HASH1, MAD_HASH2,
                DEFAULT_HASH1, DEFAULT_HASH2));
        assertEquals(-1, Utils.checkKeyHash(DEFAULT_SECTOR_KEY, SALT1,
                MAD_HASH0, MAD_HASH1, MAD_HASH2,
                DEFAULT_HASH0, DEFAULT_HASH2));
        assertEquals(-1, Utils.checkKeyHash(DEFAULT_SECTOR_KEY, SALT2,
                MAD_HASH0, MAD_HASH1, MAD_HASH2,
                DEFAULT_HASH0, DEFAULT_HASH1));

    }
}

