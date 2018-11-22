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

import java.util.Locale;

public class En1545FixedHex implements En1545Field {
    private final int mLen;
    private final String mName;

    public En1545FixedHex(String name, int len) {
        mName = name;
        mLen = len;
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    @Override
    public int parseField(byte[] b, int off, String path, En1545Parsed holder, En1545Bits bitParser) {
        StringBuilder res = new StringBuilder();
        try {
            for (int i = mLen; i > 0; ) {
                if (i >= 8) {
                    res.insert(0, String.format(Locale.ENGLISH, "%02x",
                            bitParser.getBitsFromBuffer(b, off + i - 8, 8)));
                    i -= 8;
                    continue;
                }
                if (i >= 4){
                    res.insert(0, String.format(Locale.ENGLISH, "%01x",
                            bitParser.getBitsFromBuffer(b, off + i - 4, 4)));
                    i -= 4;
                    continue;
                }
                res.insert(0, String.format(Locale.ENGLISH, "%x",
                        bitParser.getBitsFromBuffer(b, off, i)));
                break;
            }
            holder.insertString(mName, path, res.toString());
        } catch (Exception e) {
        }
        return off + mLen;
    }
}
