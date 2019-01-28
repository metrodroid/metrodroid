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

import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * EN1545 Bitmaps
 *
 * Consists of:
 * - 1 bit for every field present inside the bitmap.
 * - Where a bit is non-zero, the embedded field.
 */
class En1545Bitmap : En1545Field {
    private val mFields: List<En1545Field>
    private val mInfix: En1545Field?

    constructor(vararg fields: En1545Field) {
        this.mInfix = null
        this.mFields = fields.toList()
    }

    private constructor(infix: En1545Field, fields: List<En1545Field>) {
        this.mInfix = infix
        this.mFields = fields
    }

    override fun parseField(b: ImmutableByteArray, off: Int, path: String, holder: En1545Parsed, bitParser: En1545Bits): Int {
        var off = off
        var bitmask: Int
        try {
            bitmask = bitParser(b, off, mFields.size)
        } catch (e: Exception) {
            return off + mFields.size
        }

        off += mFields.size
        if (mInfix != null)
            off = mInfix.parseField(b, off, path, holder, bitParser)
        for (el in mFields) {
            if (bitmask and 1 != 0)
                off = el.parseField(b, off, path, holder, bitParser)
            bitmask = bitmask shr 1
        }
        return off
    }

    companion object {
        fun infixBitmap(infix: En1545Container, vararg fields: En1545Field): En1545Field {
            return En1545Bitmap(infix, fields.toList())
        }
    }
}
