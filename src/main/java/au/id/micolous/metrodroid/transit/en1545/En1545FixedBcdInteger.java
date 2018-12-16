/*
 * En1545FixedBcdInteger.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

public class En1545FixedBcdInteger implements En1545Field {
    private final int mLen;
    private final String mName;

    public En1545FixedBcdInteger(String name, int len) {
        mName = name;
        mLen = len;
    }

    @Override
    public int parseField(byte[] b, int off, String path, En1545Parsed holder, En1545Bits bitParser) {
        try {
            holder.insertInt(mName, path,
                    Utils.convertBCDtoInteger(Utils.getBitsFromBufferAsBytes(b, off, mLen)));
        } catch (Exception e) {
        }
        return off + mLen;
    }

}
