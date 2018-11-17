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

import au.id.micolous.metrodroid.util.Utils;

/**
 * EN1545 Repeated Fields
 *
 * A repeated field consists of a EN1545 Fixed Integer, containing the number of times that the
 * field value has been repeated.
 *
 * Then the field values.
 */
public class En1545Repeat implements En1545Field {
    private final En1545Field mField;
    private final int mCtrLen;

    public En1545Repeat(int ctrLen, En1545Field field) {
        mCtrLen = ctrLen;
        mField = field;
    }

    @Override
    public int parseField(byte[] b, int off, String path, En1545Parsed holder, En1545Bits bitParser) {
        int ctr;
        try {
            ctr = bitParser.getBitsFromBuffer(b, off, mCtrLen);
        } catch (Exception e) {
            return off + mCtrLen;
        }
        off += mCtrLen;
        for (int i = 0; i < ctr; i++)
            off = mField.parseField(b, off, path + "/" + Integer.toString(i), holder, bitParser);
        return off;
    }
}
