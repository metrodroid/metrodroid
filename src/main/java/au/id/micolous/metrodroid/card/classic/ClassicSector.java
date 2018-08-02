/*
 * ClassicSector.java
 *
 * Copyright 2012-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Arrays;
import java.util.List;

@Root(name = "sector")
public class ClassicSector {
    @Attribute(name = "index")
    private int mIndex;
    @ElementList(name = "blocks", required = false, empty = false)
    private List<ClassicBlock> mBlocks;
    @Attribute(name = "key", required = false)
    private Base64String mKey;

    protected ClassicSector() {
    }

    public ClassicSector(int index, ClassicBlock[] blocks, byte[] key) {
        mIndex = index;
        if (blocks == null) {
            // invalid / unauthorised sectors should be null
            mBlocks = null;
            mKey = null;
        } else {
            mBlocks = Arrays.asList(blocks);
            mKey = new Base64String(key);
        }
    }

    public int getIndex() {
        return mIndex;
    }

    public List<ClassicBlock> getBlocks() {
        return mBlocks;
    }

    public ClassicBlock getBlock(int index) throws IndexOutOfBoundsException {
        return mBlocks.get(index);
    }

    public byte[] getKey() {
        if (mKey == null) {
            return null;
        }
        return mKey.getData();
    }

    public byte[] readBlocks(int startBlock, int blockCount) throws IndexOutOfBoundsException {
        int readBlocks = 0;
        byte[] data = new byte[blockCount * 16];
        for (int index = startBlock; index < (startBlock + blockCount); index++) {
            byte[] blockData = getBlock(index).getData();
            System.arraycopy(blockData, 0, data, readBlocks * 16, blockData.length);
            readBlocks++;
        }
        return data;
    }
}
