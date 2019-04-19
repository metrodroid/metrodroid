/*
 * En1545Container.java
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

import java.util.Arrays;
import java.util.List;

import au.id.micolous.metrodroid.util.ImmutableByteArray;

/**
 * EN1545 Container
 *
 * This consists of a concatenation of all fields inside of it, with no additional data.
 */
public class En1545Container implements En1545Field {
    private final List<En1545Field> mFields;

    public En1545Container(En1545Field... fields) {
        this.mFields = Arrays.asList(fields);
    }

    @Override
    public int parseField(ImmutableByteArray b, int off, String path, En1545Parsed holder, En1545Bits bitParser) {
        for (En1545Field el : mFields) {
                off = el.parseField(b, off, path, holder, bitParser);
        }
        return off;
    }

}
