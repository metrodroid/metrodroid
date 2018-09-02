/*
 * FelicaService.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.card.felica;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Arrays;
import java.util.List;

@Root(name = "service")
public class FelicaService {
    @Attribute(name = "code")
    private int mServiceCode;
    @ElementList(name = "blocks")
    private List<FelicaBlock> mBlocks;

    private FelicaService() { /* For XML Serializer */ }

    public FelicaService(int serviceCode, FelicaBlock[] blocks) {
        mServiceCode = serviceCode;
        mBlocks = Arrays.asList(blocks);
    }

    public int getServiceCode() {
        return mServiceCode;
    }

    public List<FelicaBlock> getBlocks() {
        return mBlocks;
    }
}
