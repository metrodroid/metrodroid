/*
 * TLVTest.kt
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

class TLVTest {
    @Test
    fun testFindDefiniteShort() {
        // tag 50 (parent, definite short)
        // -> tag 51: "hello world"
        val d = ImmutableByteArray.fromHex("500e510b68656c6c6f20776f726c64")
        val e = ImmutableByteArray.fromASCII("hello world")

        assertEquals(e, ISO7816TLV.findBERTLV(d, "51", false))
    }

    @Test
    fun testFindIndefinite() {
        // tag 50 (parent, indefinite)
        // -> tag 51: "hello world"
        // end-of-contents octets
        val d = ImmutableByteArray.fromHex("5080510b68656c6c6f20776f726c640000")
        val e = ImmutableByteArray.fromASCII("hello world")

        assertEquals(e, ISO7816TLV.findBERTLV(d, "51", false))
    }

    @Test
    fun testFindDefinite1() {
        // tag 50 (parent, definite long, 1 byte)
        // -> tag 51: "hello world"
        val d = ImmutableByteArray.fromHex("50810e510b68656c6c6f20776f726c64")
        val e = ImmutableByteArray.fromASCII("hello world")

        assertEquals(e, ISO7816TLV.findBERTLV(d, "51", false))
    }

    @Test
    fun testFindDefinite7() {
        // tag 50 (parent, definite long, 7 bytes)
        // -> tag 51: "hello world"
        val d = ImmutableByteArray.fromHex("50870000000000000e510b68656c6c6f20776f726c64")
        val e = ImmutableByteArray.fromASCII("hello world")

        assertEquals(e, ISO7816TLV.findBERTLV(d, "51", false))
    }

    @Test
    fun testFindDefinite126() {
        // tag 50 (parent, definite long, 126 bytes)
        // -> tag 51: "hello world"
        val d = (ImmutableByteArray.fromHex("50fe") +
                ImmutableByteArray.empty(125) +
                ImmutableByteArray.fromHex("0e510b68656c6c6f20776f726c64"))
        val e = ImmutableByteArray.fromASCII("hello world")

        assertEquals(e, ISO7816TLV.findBERTLV(d, "51", false))
    }
}
