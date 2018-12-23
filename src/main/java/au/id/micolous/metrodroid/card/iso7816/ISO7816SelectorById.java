/*
 * ISO7816SelectorById.java
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

package au.id.micolous.metrodroid.card.iso7816;

import org.jetbrains.annotations.NonNls;
import org.simpleframework.xml.Element;

import java.io.IOException;

class ISO7816SelectorById extends ISO7816SelectorElement {
    @Element(name="id")
    @NonNls
    private int mId;

    public static final String KIND = "id";

    @Override
    byte[] select(ISO7816Protocol tag) throws IOException, ISO7816Exception {
        return tag.selectById(mId);
    }

    @Override
    @NonNls
    public String formatString() {
        return ":" + Integer.toHexString(mId);
    }

    ISO7816SelectorById() { /* for XML serializer. */ }

    ISO7816SelectorById(int id) {
        super(KIND);
        mId = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ISO7816SelectorById))
            return false;
        return ((ISO7816SelectorById) obj).mId == mId;
    }
}
