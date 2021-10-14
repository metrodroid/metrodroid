/*
 * AtrTest.kt
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

import au.id.micolous.metrodroid.card.Atr
import au.id.micolous.metrodroid.card.PCSCAtr
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AtrTest {
    @Test
    fun testFelicaAtr() {
        val felicaAtr = ImmutableByteArray.fromHex("3b8f8001804f0ca00000030611003b0000000042")
        val atr = Atr.parseAtr(felicaAtr)
        assertNotNull(atr)
        assertEquals(listOf(0, 1), atr.protocols)
        assertEquals(0x42, atr.checksumByte)

        val pcscAtr = atr.pcscAtr
        assertNotNull(pcscAtr)
        assertEquals(PCSCAtr.Standard.FELICA, pcscAtr.standard)
        assertEquals(0x11, pcscAtr.standardID)
        assertEquals(0x3b, pcscAtr.cardNameID)
    }

    @Test
    fun testDesfireAtr() {
        val desfireAtr = ImmutableByteArray.fromHex("3b8180018080")
        val atr = Atr.parseAtr(desfireAtr)

        assertNotNull(atr)
        assertEquals(listOf(0, 1), atr.protocols)
        assertEquals(0x80, atr.checksumByte)
    }

    @Test
    fun testContactAtr() {
        val contactAtr = ImmutableByteArray.fromHex("3b6e000080318066b08412016e0183009000")
        val atr = Atr.parseAtr(contactAtr)
        assertNotNull(atr)
        assertNull(atr.checksumByte)
        assertEquals(0x80, atr.cardServiceDataByte)
        assertEquals(ImmutableByteArray.fromHex("b08412016e01"), atr.preIssuingData)
        assertEquals(ImmutableByteArray.fromHex("009000"), atr.statusIndicator)
        assertEquals(listOf(0), atr.protocols)
    }

    @Test
    fun testContactlessAtr() {
        val contactlessAtr = ImmutableByteArray.fromHex("3b8e800180318066b08412016e018300900003")
        val atr = Atr.parseAtr(contactlessAtr)
        assertNotNull(atr)
        assertEquals(0x3, atr.checksumByte)
        assertEquals(0x80, atr.cardServiceDataByte)
        assertEquals(ImmutableByteArray.fromHex("b08412016e01"), atr.preIssuingData)
        assertEquals(ImmutableByteArray.fromHex("009000"), atr.statusIndicator)
        assertEquals(listOf(0, 1), atr.protocols)
    }
}
