/*
 * ISO7816Selector.java
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

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Root(name = "selector")
public class ISO7816Selector {
    @ElementList(name = "path", entry = "element")
    private List<ISO7816SelectorElement> mFullPath;

    ISO7816Selector() { /* for XML serializer. */ }

    public ISO7816Selector(List<ISO7816SelectorElement> path) {
        mFullPath = path;
    }

    public String formatString() {
        StringBuilder ret = new StringBuilder();
        for (ISO7816SelectorElement it : mFullPath) {
            ret.append(it.formatString());
        }
        return ret.toString();
    }

    static public ISO7816Selector makeSelector(int id) {
        return new ISO7816Selector(Collections.singletonList(new ISO7816SelectorById(id)));
    }

    static public ISO7816Selector makeSelector(int folder, int file) {
        return new ISO7816Selector(Arrays.asList(new ISO7816SelectorById(folder), new ISO7816SelectorById(file)));
    }

    static public ISO7816Selector makeSelector(byte []name) {
        return new ISO7816Selector(Collections.singletonList(new ISO7816SelectorByName(name)));
    }

    static public ISO7816Selector makeSelector(byte[] folder, int file) {
        return new ISO7816Selector(Arrays.asList(new ISO7816SelectorByName(folder), new ISO7816SelectorById(file)));
    }

    public void select(ISO7816Protocol tag) throws IOException {
        for (ISO7816SelectorElement sel : mFullPath) {
            sel.select(tag);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ISO7816Selector))
            return false;
        ISO7816Selector other = (ISO7816Selector) obj;
        Iterator<ISO7816SelectorElement> a = mFullPath.iterator();
        Iterator<ISO7816SelectorElement> b = other.mFullPath.iterator();
        while (true) {
            if (!a.hasNext() && !b.hasNext())
                return true;
            if (!a.hasNext() || !b.hasNext())
                return false;
            if (!a.next().equals(b.next()))
                return false;
        }
    }
}
