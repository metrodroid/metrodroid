/*
 * En1545Test.kt
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

import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class En1545Test {
    @Test
    fun testBcdDate() {
        assertEquals(Daystamp(2011, 11 /* December */, 31),
            En1545FixedInteger.parseDateBCD(0x20111231))
        assertEquals(Daystamp(2019, 0 /* January */, 1),
            En1545FixedInteger.parseDateBCD(0x20190101))
    }

    @Test
    fun testInvalidBcdDate() {
        assertNull(En1545FixedInteger.parseDateBCD(0))
        assertNull(En1545FixedInteger.parseDateBCD(-1))
    }

    @Test
    fun testBcdDateField() {
        val f = En1545Container(
            En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
            En1545FixedInteger.dateBCD(En1545TransitData.ENV_APPLICATION_VALIDITY_END))
        val h = En1545Parser.parse(
            ImmutableByteArray.fromHex("0000000020110101"), 0, f)

        assertNull(h.getTimeStamp(En1545TransitData.HOLDER_BIRTH_DATE, MetroTimeZone.UTC))
        assertEquals(Daystamp(2011, 0 /* January */, 1),
            h.getTimeStamp(En1545TransitData.ENV_APPLICATION_VALIDITY_END, MetroTimeZone.UTC))
    }
}
