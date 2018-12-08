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
package au.id.micolous.metrodroid.transit.en1545;

import java.util.Arrays;
import java.util.List;

/**
 * EN1545 Bitmaps
 *
 * Consists of:
 *  - 1 bit for every field present inside the bitmap.
 *  - Where a bit is non-zero, the embedded field.
 */
public class En1545Bitmap implements En1545Field {
    private final List<En1545Field> mFields;
    private final En1545Field mInfix;

    public En1545Bitmap(En1545Field... fields) {
        this.mInfix = null;
        this.mFields = Arrays.asList(fields);
    }

    private En1545Bitmap(En1545Field infix, List<En1545Field> fields) {
        this.mInfix = infix;
        this.mFields = fields;
    }

    public static En1545Field infixBitmap(En1545Container infix, En1545Field... fields) {
        return new En1545Bitmap(infix, Arrays.asList(fields));
    }

    @Override
    public int parseField(byte[] b, int off, String path, En1545Parsed holder, En1545Bits bitParser) {
        int bitmask;
        try {
            bitmask = bitParser.getBitsFromBuffer(b, off, mFields.size());
        } catch (Exception e) {
            return off + mFields.size();
        }
        off += mFields.size();
        if (mInfix != null)
            off = mInfix.parseField(b, off, path, holder, bitParser);
        for (En1545Field el : mFields) {
            if ((bitmask & 1) != 0)
                off = el.parseField(b, off, path, holder, bitParser);
            bitmask >>= 1;
        }
        return off;
    }
}
