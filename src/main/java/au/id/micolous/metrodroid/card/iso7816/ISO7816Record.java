/*
 * ISO7816Record.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.Locale;

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

/**
 * Represents a record in a file on a ISO7816 card.
 */
@Root(name = "record")
public class ISO7816Record {
    @Attribute(name = "index")
    private int mIndex;
    @Nullable
    @Text(required = false)
    private Base64String mData;

    ISO7816Record() { /* For XML Serializer */ }

    public ISO7816Record(int index, ImmutableByteArray data) {
        mIndex = index;
        mData = new Base64String(data);
    }

    public int getIndex() {
        return mIndex;
    }

    public ImmutableByteArray getData() {
        return mData;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ISO7816Record)) {
            return false;
        }

        ISO7816Record other = (ISO7816Record)obj;
        if (mIndex != other.mIndex) {
            return false;
        }

        return Utils.equals(mData, other.mData);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "<%d: %s>",
                mIndex, Utils.toString(mData));
    }
}
