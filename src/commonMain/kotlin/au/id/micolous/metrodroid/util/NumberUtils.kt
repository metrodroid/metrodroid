/*
 * NumberUtils.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

val Byte.hexString: String get() = NumberUtils.byteToHex(this)
val Int.hexString: String get() = NumberUtils.intToHex(this)
val Long.hexString: String get() = NumberUtils.longToHex(this)

object NumberUtils {
    fun byteToHex(v: Byte) = "0x" + (v.toInt() and 0xff).toString(16)
    fun intToHex(v: Int) = "0x" + v.toString(16)
    fun longToHex(v: Long): String = "0x" + v.toString(16)
    fun convertBCDtoInteger(data: Int): Int {
        var res = 0
        for (i in 0..7)
            res = res * 10 + ((data shr (4 * (7 - i))) and 0xf)
        return res
    }
    fun convertBCDtoInteger(data: Byte): Int = ((data.toInt() and 0xF0) shr 4) * 10 + (data.toInt() and 0x0F)
    fun zeroPad(value: Int, minDigits: Int): String {
        val cand = value.toString()
        if (cand.length >= minDigits)
            return cand
        return String(CharArray(minDigits - cand.length) { '0' }) + cand
    }
    fun zeroPad(value: Long, minDigits: Int): String {
        val cand = value.toString()
        if (cand.length >= minDigits)
            return cand
        return String(CharArray(minDigits - cand.length) { '0' }) + cand
    }

    fun groupString(value: String, separator: String, vararg groups: Int): String {
        val ret = StringBuilder()
        var ptr = 0
        for (g in groups) {
            ret.append(value, ptr, ptr + g).append(separator)
            ptr += g
        }
        ret.append(value, ptr, value.length)
        return ret.toString()
    }

    fun getDigitSum(value: Long): Int {
        var dig = value
        var digsum = 0
        while (dig > 0) {
            digsum += (dig % 10).toInt()
            dig /= 10
        }
        return digsum
    }

    fun getBitsFromInteger(buffer: Int, iStartBit: Int, iLength: Int): Int =
            (buffer shr iStartBit) and ((1 shl iLength) - 1)

    fun formatNumber(value: Long, separator: String, vararg groups: Int): String {
        val minDigit = groups.sum()
        val unformatted = zeroPad(value, minDigit)
        val numDigit = unformatted.length
        var last = numDigit - minDigit
        val ret = StringBuilder()
        ret.append(unformatted, 0, last)
        for (g in groups) {
            ret.append(unformatted, last, last + g).append(separator)
            last += g
        }
        return ret.substring(0, ret.length - 1)
    }

    private fun digitsOf(integer: Int): IntArray {
        return digitsOf(integer.toString())
    }

    fun digitsOf(integer: Long): IntArray {
        return digitsOf(integer.toString())
    }

    private fun digitsOf(integer: String): IntArray = integer.map { String(charArrayOf(it)).toInt() }.toIntArray()

    private fun luhnChecksum(cardNumber: String): Int {
        val checksum = digitsOf(cardNumber).reversed().withIndex().sumBy { (i, dig) ->
            if (i % 2 == 1)
                // we treat it as a 1-indexed array
                // so the first digit is odd
                digitsOf(dig * 2).sum()
             else
                dig
            }

        //Log.d(TAG, String.format("luhnChecksum(%s) = %d", cardNumber, checksum));
        return checksum % 10
    }

    /**
     * Given a partial card number, calculate the Luhn check digit.
     *
     * @param partialCardNumber Partial card number.
     * @return Final digit for card number.
     */
    fun calculateLuhn(partialCardNumber: String): Int {
        val checkDigit = luhnChecksum(partialCardNumber + "0")
        return if (checkDigit == 0) 0 else 10 - checkDigit
    }

    /**
     * Given a complete card number, validate the Luhn check digit.
     *
     * @param cardNumber Complete card number.
     * @return true if valid, false if invalid.
     */
    fun validateLuhn(cardNumber: String): Boolean {
        return luhnChecksum(cardNumber) == 0
    }

    fun pow(a: Int, b: Int): Long {
        var ret: Long = 1
        repeat(b) {
            ret *= a.toLong()
        }
        return ret
    }

    fun log10floor(value: Int): Int {
        var mul = 1
        var ctr = 0
        while (value >= 10 * mul) {
            ctr++
            mul *= 10
        }
        return ctr
    }

}