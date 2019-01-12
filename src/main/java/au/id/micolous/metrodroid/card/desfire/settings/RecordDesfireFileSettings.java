/*
 * RecordDesfireFileSettings.java
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

package au.id.micolous.metrodroid.card.desfire.settings;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

@Root(name = "settings")
public class RecordDesfireFileSettings extends DesfireFileSettings {
    @Element(name = "recordsize")
    private int mRecordSize;
    @Element(name = "maxrecords")
    private int mMaxRecords;
    @Element(name = "currecords")
    private int mCurRecords;

    private RecordDesfireFileSettings() { /* For XML Serializer */ }

    public RecordDesfireFileSettings(byte fileType, byte commSetting, ImmutableByteArray accessRights, int recordSize, int maxRecords, int curRecords) {
        super(fileType, commSetting, accessRights);
        this.mRecordSize = recordSize;
        this.mMaxRecords = maxRecords;
        this.mCurRecords = curRecords;
    }

    public RecordDesfireFileSettings(ImmutableByteArray buf) {
        super(buf);

        mRecordSize = buf.byteArrayToIntReversed(4, 3);
        mMaxRecords = buf.byteArrayToIntReversed(7, 3);
        mCurRecords = buf.byteArrayToIntReversed(10, 3);
    }

    public int getRecordSize() {
        return mRecordSize;
    }

    public int getMaxRecords() {
        return mMaxRecords;
    }

    public int getCurRecords() {
        return mCurRecords;
    }

    @Override
    public String getSubtitle() {
        return Utils.localizePlural(R.plurals.desfire_record_format,
                getCurRecords(),
                Utils.localizeString(getFileTypeString()),
                getCurRecords(),
                getMaxRecords(),
                getRecordSize());
    }
}
