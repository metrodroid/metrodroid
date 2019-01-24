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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.multi.Localizer;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

@Root(name = "sector")
public class ClassicSector {
    @Attribute(name = "index")
    private int mIndex;
    @ElementList(name = "blocks", required = false, empty = false)
    private List<ClassicBlock> mBlocks;
    @Attribute(name = "key", required = false)
    private Base64String mKey;
    @Attribute(name = "keytype", required = false)
    private ClassicSectorKey.KeyType mKeyType;

    protected ClassicSector() {
    }

    public ClassicSector(int index, ClassicBlock[] blocks, ClassicSectorKey key) {
        mIndex = index;
        if (blocks == null) {
            // invalid / unauthorised sectors should be null
            mBlocks = null;
            mKey = null;
            mKeyType = null;
        } else {
            mBlocks = Arrays.asList(blocks);
            mKey = new Base64String(key.getKey());
            mKeyType = key.getType();
        }
    }

    public int getIndex() {
        return mIndex;
    }

    public List<ClassicBlock> getBlocks() {
        return Collections.unmodifiableList(mBlocks);
    }

    @NonNull
    public ClassicBlock getBlock(int index) throws IndexOutOfBoundsException {
        return mBlocks.get(index);
    }

    // For kotlin []
    @NonNull
    public ClassicBlock get(int index) throws IndexOutOfBoundsException {
        return getBlock(index);
    }

    @Nullable
    public ClassicSectorKey getKey() {
        if (mKey == null) {
            return null;
        }

        return ClassicSectorKey.Companion.fromDump(mKey,
                mKeyType != null ? mKeyType : ClassicSectorKey.KeyType.UNKNOWN, "read-back");
    }

    @NonNull
    public ListItem getRawData(@NonNull String sectorIndex) {
        String key = null;
        if (mKey != null && mKeyType != null) {
            key = Localizer.INSTANCE.localizeString(mKeyType.getFormatRes(),
                    mKey.toHexString());
        } else if (mKey != null) {
            key = Localizer.INSTANCE.localizeString(R.string.classic_key_format,
                    Utils.getHexString(mKey.getData()));
        }
        List<ListItem> bli = new ArrayList<>();
        for (ClassicBlock block : getBlocks()) {
            if (block.isUnauthorized())
                bli.add(new ListItem(
                        Localizer.INSTANCE.localizeString(R.string.block_title_format_unauthorized,
                                Integer.toString(block.getIndex()))
                ));
            else
                bli.add(new ListItemRecursive(
                        Localizer.INSTANCE.localizeString(R.string.block_title_format,
                                Integer.toString(block.getIndex())),
                        block.getType(),
                        Collections.singletonList(new ListItem(null, block.getData().toHexDump())))
                );
        }
        if (isEmpty()) {
            return new ListItemRecursive(
                    Localizer.INSTANCE.localizeString(R.string.sector_title_format_empty, sectorIndex),
                    key, bli);
        }

        return new ListItemRecursive(
                Localizer.INSTANCE.localizeString(R.string.sector_title_format, sectorIndex),
                key, bli);
    }

    public ImmutableByteArray readBlocks(int startBlock, int blockCount) throws IndexOutOfBoundsException {
        ImmutableByteArray data = ImmutableByteArray.Companion.empty();
        for (int index = startBlock; index < (startBlock + blockCount); index++) {
            ImmutableByteArray blockData = getBlock(index).getData();
            data = data.plus(blockData);
        }
        return data;
    }

    public boolean isEmpty() {
        try {
            List<ClassicBlock> blocks = getBlocks();
            for (ClassicBlock block : blocks) {
                if (getIndex() == 0 && block.getIndex() == 0)
                    continue;
                if (block.getIndex() == blocks.size() - 1)
                    continue;
                if (!block.isEmpty())
                    return false;
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }
}
