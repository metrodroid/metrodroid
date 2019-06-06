package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableByteArrayTest {
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
    }
}