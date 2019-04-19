/*
 * ClassicBlock.java
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package au.id.micolous.metrodroid.card.classic;

import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "block")
public class ClassicBlock {
    public static final String TYPE_DATA = "data";
    public static final String TYPE_VALUE = "value";
    public static final String TYPE_TRAILER = "trailer";
    public static final String TYPE_MANUFACTURER = "manufacturer";

    @Attribute(name = "index")
    private int mIndex;
    @Attribute(name = "type")
    private String mType;
    @Element(name = "data")
    private Base64String mData;

    public ClassicBlock() {
    }

    public ClassicBlock(int index, String type, ImmutableByteArray data) {
        mIndex = index;
        mType = type;
        mData = new Base64String(data);
    }

    public ClassicBlock(int blockNum, String type, byte[] blockData) {
        this(blockNum, type, ImmutableByteArray.Companion.fromByteArray(blockData));
    }

    public static ClassicBlock create(@NonNls String type, int index, ImmutableByteArray data) {
        if (type.equals(TYPE_DATA) || type.equals(TYPE_VALUE)) {
            return new ClassicBlock(index, type, data);
        }
        return null;
    }

    public static ClassicBlock createUnauthorized(int index) {
        return new ClassicBlock(index, "unauthorized",
                ImmutableByteArray.Companion.fromHex("04"));
    }

    public boolean isUnauthorized() {
        return mData.toHexString().equals("04");
    }

    public int getIndex() {
        return mIndex;
    }

    public String getType() {
        return mType;
    }

    public ImmutableByteArray getData() {
        if (isUnauthorized())
            throw new UnauthorizedException();
        return mData;
    }

    private static final ImmutableByteArray ZERO = new ImmutableByteArray(16, integer -> (byte)0);
    private static final ImmutableByteArray FF = new ImmutableByteArray(16, integer -> (byte)0xff);
    private static final ImmutableByteArray ZERO_VB = ImmutableByteArray.Companion.fromHex("00000000ffffffff0000000000ff00ff");

    public boolean isEmpty() {
        if (isUnauthorized())
            throw new UnauthorizedException();
         ImmutableByteArray actual = mData;
        return actual.equals(ZERO) || actual.equals(FF) || actual.equals(ZERO_VB);
    }
}
