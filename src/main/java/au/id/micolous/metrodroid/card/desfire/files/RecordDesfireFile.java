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

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.RecordDesfireFileSettings;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

@Root(name = "file")
public class RecordDesfireFile extends DesfireFile {
    private transient List<DesfireRecord> mRecords;

    private RecordDesfireFile() { /* For XML Serializer */ }

    RecordDesfireFile(int fileId, DesfireFileSettings fileSettings, ImmutableByteArray fileData) {
        super(fileId, fileSettings, fileData);

        RecordDesfireFileSettings settings = (RecordDesfireFileSettings) fileSettings;

        DesfireRecord[] records = new DesfireRecord[settings.getCurRecords()];
        for (int i = 0; i < settings.getCurRecords(); i++) {
            int offset = settings.getRecordSize() * i;
            records[i] = new DesfireRecord(getData().sliceOffLen(offset, settings.getRecordSize()));
        }
        mRecords = Arrays.asList(records);
    }

    public List<DesfireRecord> getRecords() {
        return Collections.unmodifiableList(mRecords);
    }

    @Override
    public ListItem getRawData() {
        RecordDesfireFileSettings fileSettings = (RecordDesfireFileSettings) getFileSettings();
        if (fileSettings == null)
            return super.getRawData();

        int recSize = fileSettings.getRecordSize();
        if (recSize == 0)
            return super.getRawData();

        String title = Utils.localizeString(R.string.file_title_format,
                Utils.intToHex(getId()));
        String subtitle = getFileSettings().getSubtitle();

        List<ListItem> data = null;
        ImmutableByteArray fileData = getData();

        if (fileData != null) {
            int numRecs = (fileData.getSize() + recSize - 1) / recSize;
            data = new ArrayList<>();
            for (int i = 0; i < numRecs; i++) {
                int start = i * recSize;
                int len = recSize;
                if (start + len > fileData.getSize())
                    len = fileData.getSize() - start;
                data.add(ListItemRecursive.collapsedValue(Utils.localizeString(R.string.record_title_format, i), null,
                        fileData.sliceOffLen(start, len).toHexDump()));
            }
        }
        return new ListItemRecursive(title, subtitle, data);
    }
}
