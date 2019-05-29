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

import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * EN1545 Repeated Fields
 *
 * A repeated field consists of a EN1545 Fixed Integer, containing the number of times that the
 * field value has been repeated.
 *
 * Then the field values.
 */
class En1545Repeat(private val mCtrLen: Int, private val mField: En1545Field) : En1545Field {

    override fun parseField(b: ImmutableByteArray, off: Int, path: String, holder: En1545Parsed, bitParser: En1545Bits): Int {
        var off = off
        val ctr: Int
        try {
            ctr = bitParser(b, off, mCtrLen)
        } catch (e: Exception) {
            return off + mCtrLen
        }

        off += mCtrLen
        for (i in 0 until ctr)
            off = mField.parseField(b, off, "$path/$i", holder, bitParser)
        return off
    }
}
