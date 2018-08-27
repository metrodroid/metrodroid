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

public class En1545FixedString implements En1545Field {
    private final int mLen;
    private final String mName;

    public En1545FixedString(String name, int len) {
        mName = name;
        mLen = len;
    }

    @Override
    public int parseField(byte[] b, int off, String path, En1545Parsed holder) {
        holder.insertString(mName, path, parseString(b, off, mLen));
        return off + mLen;
    }


    public static String parseString(byte[] bin, int start, int length) {
        int i, j = 0, lastNonSpace = 0;
        StringBuilder ret = new StringBuilder();
        for (i = start; i + 4 < start + length; i += 5) {
            int bl;
            try {
                bl = Utils.getBitsFromBuffer(bin, i, 5);
            } catch (Exception e) {
                return null;
            }
            if (bl == 0 || bl == 31) {
                if (j != 0) {
                    ret.append(' ');
                    j++;
                }
            } else {
                ret.append((char) ('A' + bl - 1));
                lastNonSpace = j;
                j++;
            }
        }
        try {
            return ret.substring(0, lastNonSpace + 1);
        } catch (Exception e) {
            return ret.toString();
        }
    }
}
