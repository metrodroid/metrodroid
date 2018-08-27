/*
 * InvalidClassicSector.java
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.util.Collections;
import java.util.List;

@Root(name = "sector")
public class InvalidClassicSector extends ClassicSector {
    @SuppressWarnings("unused")
    @Attribute(name = "invalid")
    public final boolean mInvalid = true;
    @Attribute(name = "error")
    private String mError;

    public InvalidClassicSector(int index, String error) {
        super(index, null, null, null);
        mError = error;
    }

    public String getError() {
        return mError;
    }

    @Override
    public byte[] readBlocks(int startBlock, int blockCount) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException("InvalidClassicSector has no blocks");
    }

    @Override
    public List<ClassicBlock> getBlocks() {
        return Collections.emptyList();
    }

    @Override
    public ClassicBlock getBlock(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException("InvalidClassicSector has no blocks");
    }
}
