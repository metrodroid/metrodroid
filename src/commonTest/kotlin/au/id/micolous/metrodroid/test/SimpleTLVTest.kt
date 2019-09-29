/*
 * SimpleTLVTest.kt
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

import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleTLVTest {
    @Test
    fun testPCSCAtrSimpleTLV() {
        // Historical bytes from PC/SC-compatible reader on FeliCa. PC/SC specification treats the
        // historical bytes in the ATR as a Simple-TLV object, rather than a Compact-TLV object.
        val i = ImmutableByteArray.fromHex("4f0ca00000030611003b00000000")
        val expected = listOf(Pair(0x4f, ImmutableByteArray.fromHex("a00000030611003b00000000")))

        assertEquals(expected, ISO7816TLV.simpleTlvIterate(i).toList())
    }

    @Test
    fun testWithNulls() {
        val i = ImmutableByteArray.fromHex("0100020100ff00fe03112233")
        val expected = listOf(
            // Empty tag: 01
            Pair(0x02, ImmutableByteArray.fromHex("00")),
            // Empty tag: FF
            Pair(0xfe, ImmutableByteArray.fromHex("112233"))
        )

        assertEquals(expected, ISO7816TLV.simpleTlvIterate(i).toList())
    }

    @Test
    fun testLongLength() {
        val i = ImmutableByteArray.fromHex("0fff00031122330a000b0211220cff00000d0122")
        val expected = listOf(
            // Long length = 5 bytes
            Pair(0x0f, ImmutableByteArray.fromHex("112233")),
            // Empty tag: 0A
            Pair(0x0b, ImmutableByteArray.fromHex("1122")),
            // Empty long tag: 0C
            Pair(0x0d, ImmutableByteArray.fromHex("22"))
        )

        assertEquals(expected, ISO7816TLV.simpleTlvIterate(i).toList())
    }
}
