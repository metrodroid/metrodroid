/*
 * ISO7816File.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import android.support.annotation.Nullable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.xml.HexString;

/**
 * Represents an application (DF) on an ISO7816 card.
 */
@Root(name = "file")
public class ISO7816File {
    @Attribute(name = "name")
    private HexString mName;

    @Element(name = "data", required = false)
    private Base64String mBinaryData;

    @ElementList(name = "records", required = false, empty = false)
    private List<ISO7816Record> mRecords;



    ISO7816File() { /* For XML Serializer */ }

    ISO7816File(byte[] fileId, @Nullable byte[] binaryData, @Nullable List<ISO7816Record> records) {
        mName = new HexString(fileId);

        if (binaryData != null) {
            mBinaryData = new Base64String(binaryData);
        }

        if (records != null) {
            mRecords = records;
        }
    }


    public byte[] getFileName() {
        return mName.getData();
    }

    @Nullable
    public byte[] getBinaryData() {
        if (mBinaryData != null) {
            return mBinaryData.getData();
        } else {
            return null;
        }
    }

    @Nullable
    public List<ISO7816Record> getRecords() {
        return mRecords;
    }

    /**
     * Gets a record for a given index.
     * @param index Record index to retrieve.
     * @return ISO7816Record with that index, or null if not present.
     */
    public ISO7816Record getRecord(int index) {
        for (ISO7816Record record : mRecords) {
            if (record.getIndex() == index) {
                return record;
            }
        }

        return null;
    }

}