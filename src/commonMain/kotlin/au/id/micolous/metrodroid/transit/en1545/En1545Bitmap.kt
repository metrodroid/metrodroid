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
 * EN1545 Bitmaps
 *
 * Consists of:
 * - 1 bit for every field present inside the bitmap.
 * - Where a bit is non-zero, the embedded field.
 */
class En1545Bitmap private constructor(
        private val mInfix: En1545Field?,
        private val mFields: List<En1545Field>,
        private val reversed: Boolean
): En1545Field {

    constructor(vararg fields: En1545Field, reversed: Boolean = false) : this(
        mInfix = null,
        mFields = fields.toList(),
        reversed = reversed
    )

    @Suppress("NAME_SHADOWING")
    override fun parseField(b: ImmutableByteArray, off: Int, path: String, holder: En1545Parsed, bitParser: En1545Bits): Int {
        var off = off
        val bitmask: Int
        try {
            bitmask = bitParser(b, off, mFields.size)
        } catch (e: Exception) {
            return off + mFields.size
        }

        off += mFields.size
        if (mInfix != null)
            off = mInfix.parseField(b, off, path, holder, bitParser)
        var curbit = if (reversed) (1 shl (mFields.size - 1)) else 1
        for (el in mFields) {
            if (bitmask and curbit != 0)
                off = el.parseField(b, off, path, holder, bitParser)
            curbit = if (reversed) curbit shr 1 else curbit shl 1
        }
        return off
    }

    companion object {
        fun infixBitmap(infix: En1545Container, vararg fields: En1545Field, reversed: Boolean = false): En1545Field
                = En1545Bitmap(infix, fields.toList(), reversed = reversed)
    }
}
