/*
 * TLVDroidTest.kt
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
import kotlin.test.assertNull

class TLVDroidTest : BaseInstrumentedTest() {
    @Test
    fun testFindDefiniteReallyLong() {
        // TODO: Move into common tests
        // This calls Log.e (by design), which fails on Android mock environments.

        // tag 50 (parent, definite long, 0xffffffffffffffff bytes)
        // -> tag 51: "hello world"
        val d = (ImmutableByteArray.fromHex("5088") +
                ImmutableByteArray(8) { 0xff.toByte() } +
                ImmutableByteArray.fromHex("0e510b68656c6c6f20776f726c64"))

        // Should fail
        assertNull(ISO7816TLV.findBERTLV(d, "51", false))
    }
}