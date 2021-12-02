/*
 * ImmutableByteArrayTest.kt
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

import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.*

class ImmutableByteArrayTest : BaseInstrumentedTest() {
    @Test
    fun testIndexOfAtStart() {
        // Check single byte
        val s = ImmutableByteArray.fromHex("0102030405")
        val n = ImmutableByteArray.fromHex("01")

        assertEquals(0, s.indexOf(n))
        assertEquals(-1, s.indexOf(n, 1))
        assertEquals(-1, s.indexOf(n, end = 0))

        // Check multiple bytes
        val n2 = ImmutableByteArray.fromHex("010203")
        assertEquals(0, s.indexOf(n2))
        assertEquals(-1, s.indexOf(n2, 1))

        // Check whole bytes match
        assertEquals(0, s.indexOf(s))
        assertEquals(-1, s.indexOf(s, 1))
        assertEquals(-1, s.indexOf(s, end = 1))
        assertEquals(-1, s.indexOf(s, end = 2))

        assertEquals(0, s.indexOf(ImmutableByteArray.empty()))
        assertEquals(1, s.indexOf(ImmutableByteArray.empty(), start = 1))
        assertEquals(-1, s.indexOf(ImmutableByteArray.empty(), start = -1))
        assertEquals(-1, s.indexOf(ImmutableByteArray.empty(), start = 0, end = 10))
        assertEquals(-1, s.indexOf(ImmutableByteArray.empty(), start = 10))
        assertEquals(-1, s.indexOf(ImmutableByteArray.empty(), start = 1, end = 0))
    }

    @Test
    fun testIndexOfAtMiddle() {
        // Check single byte
        val s = ImmutableByteArray.fromHex("0102030405")
        val n = ImmutableByteArray.fromHex("03")

        assertEquals(2, s.indexOf(n))
        assertEquals(2, s.indexOf(n, 1))
        assertEquals(2, s.indexOf(n, 2))
        assertEquals(-1, s.indexOf(n, 3))
        assertEquals(-1, s.indexOf(n, end = 0))
        assertEquals(-1, s.indexOf(n, end = 1))
        assertEquals(-1, s.indexOf(n, end = 2))

        // Check multiple bytes
        val n2 = ImmutableByteArray.fromHex("0304")
        assertEquals(2, s.indexOf(n2))
        assertEquals(2, s.indexOf(n2, 1))
        assertEquals(2, s.indexOf(n2, 2))
        assertEquals(-1, s.indexOf(n2, 3))

        assertEquals(-1, s.indexOf(n2, end = 0))
        assertEquals(-1, s.indexOf(n2, end = 1))
        assertEquals(-1, s.indexOf(n2, end = 2))

        assertEquals(-1, s.indexOf(n2, 1, 1))
        assertEquals(-1, s.indexOf(n2, 1, 2))
        assertEquals(-1, s.indexOf(n2, 1, 3))
        assertEquals(2, s.indexOf(n2, 1, 4))
        assertEquals(2, s.indexOf(n2, 1, 5))

        assertEquals(-1, s.indexOf(n2, 2, 2))
        assertEquals(-1, s.indexOf(n2, 2, 3))
        assertEquals(2, s.indexOf(n2, 2, 4))
        assertEquals(2, s.indexOf(n2, 2, 5))

        assertEquals(-1, s.indexOf(n2, 3, 4))
        assertEquals(-1, s.indexOf(n2, 3, 5))
        assertEquals(-1, s.indexOf(n2, 3, 6))

        assertEquals(2, ImmutableByteArray.fromASCII("ABABABCD")
            .indexOf(ImmutableByteArray.fromASCII("ABABCD")))
        assertEquals(3, ImmutableByteArray.fromASCII("ABCABCDABCD")
            .indexOf(ImmutableByteArray.fromASCII("ABCDABCD")))
    }

    @Test
    fun testBase64() {
        assertEquals(ImmutableByteArray.fromASCII("Metrodroid"),
            ImmutableByteArray.fromBase64("TWV0cm9kcm9pZA=="))
        assertEquals(ImmutableByteArray.fromASCII("Metrodroid"),
            ImmutableByteArray.fromBase64("TWV-0cm9k-cm9p----ZA==--"))
        assertFails { ImmutableByteArray.fromBase64("Metrodroid") } // Wrong padding
        assertFails { ImmutableByteArray.fromBase64("====") } // Wrong padding
    }

    @Test
    fun testMap() {
        assertEquals(ImmutableByteArray.fromHex("00010409101924"),
            ImmutableByteArray.fromHex("00010203040506").map { (it * it).toByte() })
    }

    @Test
    fun testContains() {
        assertTrue(ImmutableByteArray.fromHex("00010203040506").contains(0))
        assertTrue(ImmutableByteArray.fromHex("00010203040506").contains(1))
        assertFalse(ImmutableByteArray.fromHex("00010203040506").contains(7))
        assertTrue(ImmutableByteArray.fromHex("00010203040506ff").contains(-1))
        assertTrue(ImmutableByteArray.fromHex("00010203040506ff").contains(0xff.toByte()))
        assertTrue(ImmutableByteArray.fromHex("00010203040506").containsAll(listOf(0, 1)))
        assertTrue(ImmutableByteArray.fromHex("00010203040506").containsAll(listOf(1, 0)))
        assertTrue(ImmutableByteArray.fromHex("00010203040506").containsAll(listOf(0)))
        assertFalse(ImmutableByteArray.fromHex("00010203040506").containsAll(listOf(0, 7)))
    }

    @Test
    fun testCopyInto() {
        val b1 = "ZZZZZZZZZ".encodeToByteArray()
        ImmutableByteArray.fromASCII("XXABCDXX").copyInto(b1, 1, 2, 5)
        assertContentEquals("ZABCZZZZZ".encodeToByteArray(), b1)

        val b2 = "ZZZZZZZZZ".encodeToByteArray()
        ImmutableByteArray.fromASCII("XXABCDXX").copyInto(b2, 1, 2)
        assertContentEquals("ZABCDXXZZ".encodeToByteArray(), b2)

        val b3 = "ZZZZZZZZZ".encodeToByteArray()
        ImmutableByteArray.fromASCII("XXABCDXX").copyInto(b3)
        assertContentEquals("XXABCDXXZ".encodeToByteArray(), b3)
    }

    @Test
    fun testIsASCII() {
        assertTrue(ImmutableByteArray.fromUTF8("ABC").isASCII())
        assertFalse(ImmutableByteArray.fromUTF8("ABC\u0420ABC").isASCII())
        assertFalse(ImmutableByteArray.fromUTF8("ABC\u0004ABC").isASCII())
        assertTrue(ImmutableByteArray.fromUTF8("ABC\nABC").isASCII())
        assertTrue(ImmutableByteArray.fromUTF8("ABC\r\nABC").isASCII())
        assertFalse(ImmutableByteArray.fromUTF8("ABC\u0000ABC").isASCII())
        assertFalse(ImmutableByteArray.fromUTF8("ABC\u000BABC").isASCII())
        assertFalse(ImmutableByteArray.fromUTF8("ABC\u000FABC").isASCII())
    }

    @Test
    fun testLatin1() {
        assertEquals("1fª»Ì", ImmutableByteArray.fromHex("316600AABBCC").readLatin1())
    }

    @Test
    fun testEquals() {
        assertEquals(ImmutableByteArray.fromHex("31"), ImmutableByteArray.fromASCII("1"))
        assertFalse(ImmutableByteArray.fromASCII("1").equals(object {}))
    }

    @Test
    fun testStartsWith() {
        assertTrue(ImmutableByteArray.fromASCII("ABC").startsWith(
            ImmutableByteArray.fromASCII("AB")))
        assertFalse(ImmutableByteArray.fromASCII("ABC").startsWith(
            ImmutableByteArray.fromASCII("ABCD")))
        assertFalse(ImmutableByteArray.fromASCII("ABC").startsWith(
            ImmutableByteArray.fromASCII("ABD")))
        assertFalse(ImmutableByteArray.fromASCII("ABC").startsWith(
            ImmutableByteArray.fromASCII("AD")))
        assertFalse(ImmutableByteArray.fromASCII("ABC").startsWith(
            ImmutableByteArray.fromASCII("BCD")))
    }

    @Test
    fun testSignedLeBits() {
        assertEquals(-14,
            ImmutableByteArray.fromASCII("ABC").getBitsFromBufferSignedLeBits(5, 5))
        assertEquals(4,
            ImmutableByteArray.fromASCII("ABC").getBitsFromBufferSignedLeBits(7, 5))
        assertEquals(-494,
            ImmutableByteArray.fromASCII("ABC").getBitsFromBufferSignedLeBits(5, 10))
    }

    @Test
    fun testSlice() {
        assertEquals(ImmutableByteArray.fromASCII("BCD"),
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLen(1, 3))
        assertEquals(ImmutableByteArray.fromASCII("BCD"),
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLenSafe(1, 3))
        assertFails {
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLen(-1, 3)
        }
        assertEquals(ImmutableByteArray.empty(),
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLen(3, -1))
        assertFails {
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLen(3, 7)
        }
        assertFails {
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLen(7, 1)
        }
        assertEquals(null,
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLenSafe(-1, 3))
        assertEquals(null,
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLenSafe(3, -1))
        assertEquals(ImmutableByteArray.fromASCII("DE"),
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLenSafe(3, 7))
        assertEquals(null,
            ImmutableByteArray.fromASCII("ABCDE").sliceOffLenSafe(7, 1))
    }

    @Test
    fun testPlus() {
        assertEquals(ImmutableByteArray.fromASCII("ABCDE"),
            ImmutableByteArray.fromASCII("ABC") + ImmutableByteArray.fromASCII("DE"))
        assertEquals(ImmutableByteArray.fromASCII("ABCDE"),
            ImmutableByteArray.fromASCII("ABCD") + 'E'.code.toByte())
        assertEquals(ImmutableByteArray.fromASCII("ABCDE"),
            ImmutableByteArray.fromASCII("ABC") + "DE".encodeToByteArray())
    }

    @Test
    fun testHex() {
        assertFailsWith<IllegalArgumentException> {
            ImmutableByteArray.fromHex("0")
        }
        assertFailsWith<IllegalArgumentException> {
            ImmutableByteArray.fromHex("1X")
        }
        assertEquals("41", ImmutableByteArray.fromASCII("A").toHexString())
        assertEquals("41", ImmutableByteArray.getHexString("A".encodeToByteArray()))
        assertEquals("010203",
            ImmutableByteArray.fromHex("010203").toHexDump().unformatted)
        assertEquals("01020304",
            ImmutableByteArray.fromHex("01020304").toHexDump().unformatted)
        assertEquals("01020304 05",
            ImmutableByteArray.fromHex("0102030405").toHexDump().unformatted)
        assertEquals("01020304 05060708",
            ImmutableByteArray.fromHex("0102030405060708").toHexDump().unformatted)
        assertEquals("01020304 05060708 090a0b0c 0d0e0f00",
            ImmutableByteArray.fromHex("0102030405060708090a0b0c0d0e0f00").toHexDump().unformatted)
        assertEquals("01020304 05060708 090a0b0c 0d0e0f00\n11",
            ImmutableByteArray.fromHex("0102030405060708090a0b0c0d0e0f0011").toHexDump().unformatted)
    }

    @Test
    fun testInt() {
        assertEquals(0x0102030405060708,
            ImmutableByteArray.byteArrayToLong(byteArrayOf(1,2,3,4,5,6,7,8), 0, 8))
        assertFailsWith<IllegalArgumentException> {
            ImmutableByteArray.byteArrayToLong(byteArrayOf(1,2,3,4,5,6,7,8), 0, 9)
        }
        assertEquals(0x0102030405060708,
            ImmutableByteArray.of(1,2,3,4,5,6,7,8).byteArrayToLong(0, 8))
        assertEquals(0x0102030405060708,
            ImmutableByteArray.of(1,2,3,4,5,6,7,8).byteArrayToLong())
        assertEquals(0x0807060504030201,
            ImmutableByteArray.of(1,2,3,4,5,6,7,8).byteArrayToLongReversed())
        assertEquals(0x01020304,
            ImmutableByteArray.of(1,2,3,4).byteArrayToInt())
        assertEquals(0x04030201,
            ImmutableByteArray.of(1,2,3,4).byteArrayToIntReversed())
        assertFailsWith<IllegalArgumentException> {
            ImmutableByteArray.of(1,2,3,4,5,6,7,8).byteArrayToLong(0, 9)
        }
    }

    @Test
    fun testCompare() {
        assertEquals(0, ImmutableByteArray.fromASCII("ABCD").compareTo(ImmutableByteArray.fromASCII("ABCD")))
        assertTrue(ImmutableByteArray.fromASCII("ABCD") < ImmutableByteArray.fromASCII("ABCE"))
        assertTrue(ImmutableByteArray.fromASCII("ABCD") > ImmutableByteArray.fromASCII("ABCC"))
        assertTrue(ImmutableByteArray.fromASCII("ABCD") < ImmutableByteArray.fromASCII("ABCDE"))
        assertTrue(ImmutableByteArray.fromASCII("ABCD") > ImmutableByteArray.fromASCII("AB\u0420"))
        assertTrue(ImmutableByteArray.fromASCII("AB\u0420") < ImmutableByteArray.fromASCII("AB\u0421"))
    }

    @Test
    fun testCopyConstructor() {
        assertEquals(ImmutableByteArray.fromHex("ABCD"),
            ImmutableByteArray(ImmutableByteArray.fromHex("ABCD")))
    }

    @Test
    fun testAny() {
        assertTrue(ImmutableByteArray.fromASCII("ABCD").any { it == 'A'.code.toByte() })
        assertFalse(ImmutableByteArray.fromASCII("ABCD").any { it == 'a'.code.toByte() })
    }
}
