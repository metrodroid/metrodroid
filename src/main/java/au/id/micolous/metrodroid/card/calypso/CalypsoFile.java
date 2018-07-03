/*
 * CalypsoFile.java
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
package au.id.micolous.metrodroid.card.calypso;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * Represents a file on a Calypso card.
 */
@Root(name = "file")
public class CalypsoFile {
    @Attribute(name = "file")
    private int mFileId;
    @Attribute(name = "folder")
    private int mFolderId;
    @ElementList(name = "records", required = false, empty = false)
    private List<CalypsoRecord> mRecords;

    CalypsoFile() { /* For XML Serializer */ }

    CalypsoFile(int folderId, int fileId, List<CalypsoRecord> records) {
        mFolderId = folderId;
        mFileId = fileId;
        mRecords = records;
    }

    public int getFile() {
        return mFileId;
    }

    public int getFolder() {
        return mFolderId;
    }

    public List<CalypsoRecord> getRecords() {
        return mRecords;
    }

    /**
     * Gets a record for a given index.
     * @param index Record index to retrieve.
     * @return CalypsoRecord with that index, or null if not present.
     */
    public CalypsoRecord getRecord(int index) {
        for (CalypsoRecord record : mRecords) {
            if (record.getIndex() == index) {
                return record;
            }
        }

        return null;
    }
}
