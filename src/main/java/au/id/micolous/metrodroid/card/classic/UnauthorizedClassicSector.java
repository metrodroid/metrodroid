/*
 * UnauthorizedClassicSector.java
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

import au.id.micolous.metrodroid.card.UnauthorizedException;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "sector")
public class UnauthorizedClassicSector extends ClassicSector {
    @SuppressWarnings("unused")
    @Attribute(name = "unauthorized")
    public static final boolean UNAUTHORIZED = true;

    private UnauthorizedClassicSector() { /* For XML serializer */ }

    public UnauthorizedClassicSector(int sectorIndex) {
        super(sectorIndex, null, null, null);
    }

    @Override
    public byte[] readBlocks(int startBlock, int blockCount) {
        throw new UnauthorizedException();
    }

    @Override
    public List<ClassicBlock> getBlocks() {
        throw new UnauthorizedException();
    }

    @Override
    public ClassicBlock getBlock(int index) {
        throw new UnauthorizedException();
    }
}
