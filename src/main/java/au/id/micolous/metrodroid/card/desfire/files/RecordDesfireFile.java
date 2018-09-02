/*
 * RecordDesfireFile.java
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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
package au.id.micolous.metrodroid.card.desfire.files;

import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.RecordDesfireFileSettings;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Root;

import java.util.Arrays;
import java.util.List;

@Root(name = "file")
public class RecordDesfireFile extends DesfireFile {
    private transient List<DesfireRecord> mRecords;

    private RecordDesfireFile() { /* For XML Serializer */ }

    RecordDesfireFile(int fileId, DesfireFileSettings fileSettings, byte[] fileData) {
        super(fileId, fileSettings, fileData);

        RecordDesfireFileSettings settings = (RecordDesfireFileSettings) fileSettings;

        DesfireRecord[] records = new DesfireRecord[settings.getCurRecords()];
        for (int i = 0; i < settings.getCurRecords(); i++) {
            int offset = settings.getRecordSize() * i;
            records[i] = new DesfireRecord(ArrayUtils.subarray(getData(), offset, offset + settings.getRecordSize()));
        }
        mRecords = Arrays.asList(records);
    }

    public List<DesfireRecord> getRecords() {
        return mRecords;
    }
}
