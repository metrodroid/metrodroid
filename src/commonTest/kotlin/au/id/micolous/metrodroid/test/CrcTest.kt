/*
 * CrcTest.kt
 *
 * Copyright 2019 Google
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

import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class CrcTest {
    @Test
    fun testIBM() {
        assertEquals(actual=HashUtils.calculateCRC16IBM(ImmutableByteArray.empty()), expected=0x0000)
        assertEquals(actual=HashUtils.calculateCRC16IBM(ImmutableByteArray.of (1)), expected=0xc0c1)
        assertEquals(actual=HashUtils.calculateCRC16IBM(ImmutableByteArray.fromASCII("IBM")), expected=0x4321)
        assertEquals(actual=HashUtils.calculateCRC16IBM(ImmutableByteArray.fromASCII("Metrodroid")), expected=0xe52c)
        assertEquals(actual=HashUtils.calculateCRC16IBM(ImmutableByteArray.fromASCII("CrcTest")), expected=0x2699)
    }
}
