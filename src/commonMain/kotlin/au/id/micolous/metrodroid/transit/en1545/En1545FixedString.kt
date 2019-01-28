/*
 * En1545Fixed.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.en1545

import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray

class En1545FixedString(private val mName: String, private val mLen: Int) : En1545Field {

    override fun parseField(b: ImmutableByteArray, off: Int, path: String, holder: En1545Parsed, bitParser: En1545Bits): Int {
        val string = parseString(b, off, mLen, bitParser)
        if (string != null)
            holder.insertString(mName, path, string)
        return off + mLen
    }

    private fun parseString(bin: ImmutableByteArray, start: Int, length: Int, bitParser: En1545Bits): String? {
        var i: Int = start
        var j = 0
        var lastNonSpace = 0
        val ret = StringBuilder()
        while (i + 4 < start + length && i + 4 < bin.size * 8) {
            val bl: Int
            try {
                bl = bitParser(bin, i, 5)
            } catch (e: Exception) {
                Log.e(TAG, "parseString failed", e)
                return null
            }

            if (bl == 0 || bl == 31) {
                if (j != 0) {
                    ret.append(' ')
                    j++
                }
            } else {
                ret.append(('A'.toInt() + bl - 1).toChar())
                lastNonSpace = j
                j++
            }
            i += 5
        }
        try {
            return ret.substring(0, lastNonSpace + 1)
        } catch (e: Exception) {
            return ret.toString()
        }
    }

    companion object {
        private const val TAG = "En1545FixedString"
    }
}
