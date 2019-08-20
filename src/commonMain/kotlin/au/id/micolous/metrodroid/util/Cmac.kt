/*
 * Cmac.kt
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

package au.id.micolous.metrodroid.util

object Cmac {
    private fun cmacPad(input: ImmutableByteArray) = input + ImmutableByteArray.of(0x80.toByte()) +
            ImmutableByteArray(16 - input.size - 1) { 0.toByte() }

    private fun shl1(input: ImmutableByteArray) = ImmutableByteArray(input.size) {
        ((input[it].toInt() shl 1) or ((if (it == input.size - 1) 0 else input[it + 1].toInt() and 0x80) shr 7)).toByte()
    }

    private fun cmacSubkeys(cipher: (ImmutableByteArray) -> ImmutableByteArray): Pair<ImmutableByteArray, ImmutableByteArray> {
        val l = cipher(ImmutableByteArray.empty(16))

        val rb = ImmutableByteArray.fromHex("00000000000000000000000000000087")
        val k1 = if (l[0].toInt() and 0x80 == 0) {
            shl1(l)
        } else {
            shl1(l) xor rb
        }
        val k2 = if (k1[0].toInt() and 0x80 == 0) {
            shl1(k1)
        } else {
            shl1(k1) xor rb
        }
        return Pair(k1, k2)
    }

    private fun cmac(macdata: ImmutableByteArray, cipher: (ImmutableByteArray) -> ImmutableByteArray): ImmutableByteArray {
        var x = ImmutableByteArray.empty(16)
        val (k1, k2) = cmacSubkeys(cipher)
        val n = (macdata.size + 15) / 16
        for (i in 0..(n - 2)) {
            x = cipher(x xor macdata.sliceOffLen(16 * i, 16))
        }
        val lastBlockStart = (n - 1) * 16
        val lastBlock = macdata.sliceOffLen(lastBlockStart, macdata.size - lastBlockStart)
        val mlast = if (lastBlock.size == 16) lastBlock xor k1 else cmacPad(lastBlock) xor k2
        return cipher(mlast xor x)
    }

    fun aesCmac(macdata: ImmutableByteArray, key: ImmutableByteArray): ImmutableByteArray = cmac(
            macdata) { Aes.encryptCbc(it, key)}
}
