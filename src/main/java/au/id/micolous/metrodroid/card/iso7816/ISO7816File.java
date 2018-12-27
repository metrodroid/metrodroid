/*
 * ISO7816File.java
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
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

/**
 * Represents a file on a Calypso card.
 */
@Root(name = "file")
public class ISO7816File {
    @SuppressWarnings("FieldCanBeLocal")
    @Attribute(name = "name", required = false)
    private String mReadableName;

    @Element(name = "selector", required = false)
    private ISO7816Selector mSelector;

    @NonNull
    @Element(name = "data", required = false)
    private Base64String mBinaryData;

    @NonNull
    @ElementList(name = "records", required = false, empty = false)
    private List<ISO7816Record> mRecords;

    @NonNull
    @Element(name = "fci", required = false)
    private Base64String mFci;

    ISO7816File() {
        /* For XML Serializer */
        mBinaryData = Base64String.Companion.empty();
        mFci = Base64String.Companion.empty();
    }

    ISO7816File(@Nullable ISO7816Selector selector,
                @Nullable List<ISO7816Record> records,
                @NonNull byte[] binaryData,
                @Nullable byte[] fci) {
        mRecords = records != null ? records : Collections.emptyList();
        mBinaryData = new Base64String(binaryData);
        mFci = new Base64String(fci);

        mSelector = selector;
        mReadableName = mSelector == null ? null : mSelector.formatString();
    }

    public List<ISO7816Record> getRecords() {
        Collections.sort(mRecords, (a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
        return Collections.unmodifiableList(mRecords);
    }

    @Nullable
    public ImmutableByteArray getBinaryData() {
        return mBinaryData;
    }

    @Nullable
    public ImmutableByteArray getFci() {
        return mFci;
    }

    /**
     * Gets a record for a given index.
     * @param index Record index to retrieve.
     * @return ISO7816Record with that index, or null if not present.
     */
    @Nullable
    public ISO7816Record getRecord(int index) {
        for (ISO7816Record record : mRecords) {
            if (record.getIndex() == index) {
                return record;
            }
        }

        return null;
    }

    public ISO7816Selector getSelector() {
        return mSelector;
    }

    public ListItem showRawData(String selectorStr) {
        List<ListItem> recList = new ArrayList<>();
        ImmutableByteArray binaryData = getBinaryData();
        ImmutableByteArray fciData = getFci();
        if (binaryData != null)
            recList.add(ListItemRecursive.collapsedValue(Utils.localizeString(R.string.binary_title_format),
                    binaryData.toHexDump()));
        if (fciData != null)
            recList.add(new ListItemRecursive(Utils.localizeString(R.string.file_fci), null,
                    ISO7816TLV.INSTANCE.infoWithRaw(fciData)));
        List<ISO7816Record> records = getRecords();
        for (ISO7816Record record : records)
            recList.add(ListItemRecursive.collapsedValue(Utils.localizeString(R.string.record_title_format, record.getIndex()),
                    record.getData().toHexDump()));
        return new ListItemRecursive(Utils.localizeString(R.string.file_title_format, selectorStr),
                Utils.localizePlural(R.plurals.record_count, records.size(), records.size()),
                recList);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ISO7816File)) {
            return false;
        }

        ISO7816File other = (ISO7816File)obj;

        // Readable Name isn't important

        if (!Utils.equals(mSelector, other.mSelector)) {
            return false;
        }

        if (!mBinaryData.equals(other.mBinaryData)) {
            return false;
        }

        if (!mRecords.equals(other.mRecords)) {
            return false;
        }

        return mFci.equals(other.mFci);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "<%s: %s, data=%s, records=%s>",
                getClass().getSimpleName(), Utils.toString(mSelector), mBinaryData, mRecords);
    }
}
