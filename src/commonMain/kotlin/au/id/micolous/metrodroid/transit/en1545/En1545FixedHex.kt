/*
 * En1545Fixed.kt
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

class En1545FixedHex(private val mName: String, private val mLen: Int) : En1545Field {

    override fun parseField(b: ImmutableByteArray, off: Int, path: String, holder: En1545Parsed, bitParser: En1545Bits): Int {
        var res = ""
        try {
            var i = mLen
            while (i > 0) {
                if (i >= 8) {
                    var t = bitParser(b, off + i - 8, 8).toString(16)
                    if (t.length == 1)
                        t = "0$t"
                    res = t + res
                    i -= 8
                    continue
                }
                if (i >= 4) {
                    res = bitParser(b, off + i - 4, 4).toString(16) + res
                    i -= 4
                    continue
                }
                res = bitParser(b, off, i).toString(16) + res
                break
            }
            holder.insertString(mName, path, res)
        } catch (e: Exception) {
            Log.w("En1545FixedHex", "Overflow when parsing en1545", e)
        }

        return off + mLen
    }
}
