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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import au.id.micolous.metrodroid.util.ImmutableByteArray;

@Root(name = "selector")
public class ISO7816Selector {
    @ElementList(name = "path", entry = "element")
    private final List<ISO7816SelectorElement> mFullPath;

    ISO7816Selector() { /* for XML serializer. */ mFullPath = new ArrayList<>(); }

    public ISO7816Selector(@ElementList(name = "path", entry = "element") @NonNull
                                   List<ISO7816SelectorElement> path) {
        mFullPath = path;
    }

    @NonNull
    public static ISO7816Selector makeSelector(int... path) {
        List<ISO7816SelectorElement> sels = new ArrayList<>();
        for (int el : path)
            sels.add(new ISO7816SelectorById(el));
        return new ISO7816Selector(sels);
    }

    @NonNull
    public String formatString() {
        StringBuilder ret = new StringBuilder();
        for (ISO7816SelectorElement it : mFullPath) {
            ret.append(it.formatString());
        }
        return ret.toString();
    }

    @NonNull
    static public ISO7816Selector makeSelector(ImmutableByteArray name) {
        return new ISO7816Selector(Collections.singletonList(new ISO7816SelectorByName(name)));
    }

    @NonNull
    static public ISO7816Selector makeSelector(ImmutableByteArray folder, int file) {
        return new ISO7816Selector(Arrays.asList(new ISO7816SelectorByName(folder), new ISO7816SelectorById(file)));
    }

    @Nullable
    public ImmutableByteArray select(ISO7816Protocol tag) throws IOException, ISO7816Exception {
        ImmutableByteArray fci = null;
        for (ISO7816SelectorElement sel : mFullPath) {
            fci = sel.select(tag);
        }
        return fci;
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

    /**
     * If this selector starts with (or is the same as) {@param other}, return true.
     *
     * @param other The other selector to compare with.
     * @return True if this starts with {@param other}.
     */
    public boolean startsWith(@NonNull ISO7816Selector other) {
        Iterator<ISO7816SelectorElement> a = mFullPath.iterator();
        Iterator<ISO7816SelectorElement> b = other.mFullPath.iterator();

        while (true) {
            if (!b.hasNext())
                return true; // "other" is shorter or equal length to this
            if (!a.hasNext())
                return false; // "other" is longer
            if (!a.next().equals(b.next()))
                return false;
        }
    }

    @NonNull
    public ISO7816Selector appendPath(int... path) {
        List<ISO7816SelectorElement> sels = new ArrayList<>(mFullPath);
        for (int el : path) {
            sels.add(new ISO7816SelectorById(el));
        }
        return new ISO7816Selector(sels);
    }

    /**
     * Returns the number of {@link ISO7816SelectorElement}s in this {@link ISO7816Selector}.
     */
    public int size() {
        return mFullPath.size();
    }

    /**
     * Returns the parent selector, or <code>null</code> if at the root (or 1 level from the root).
     *
     * @return The parent of the path selector represented by this {@link ISO7816Selector}.
     */
    @Nullable
    public ISO7816Selector parent() {
        List<ISO7816SelectorElement> path = mFullPath;
        if (path.size() <= 1) {
            return null;
        }

        path = new ArrayList<>(path);
        path.remove(path.size() - 1);
        return new ISO7816Selector(path);
    }

    @NonNull
    @Override
    public String toString() {
        return formatString();
    }
}
