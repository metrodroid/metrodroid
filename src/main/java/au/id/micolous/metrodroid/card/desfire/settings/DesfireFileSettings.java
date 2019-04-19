/*
 * DesfireFileSettings.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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

import android.support.annotation.StringRes;

import org.simpleframework.xml.Element;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.HexString;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public abstract class DesfireFileSettings {
    /* DesfireFile Types */
    public static final byte STANDARD_DATA_FILE = (byte) 0x00;
    public static final byte BACKUP_DATA_FILE = (byte) 0x01;
    public static final byte VALUE_FILE = (byte) 0x02;
    public static final byte LINEAR_RECORD_FILE = (byte) 0x03;
    public static final byte CYCLIC_RECORD_FILE = (byte) 0x04;
    @Element(name = "filetype")
    private byte mFileType;
    @Element(name = "commsettings")
    private byte mCommSetting;
    @Element(name = "accessrights")
    private HexString mAccessRights;

    DesfireFileSettings() { /* For XML Serializer */ }

    DesfireFileSettings(ImmutableByteArray settings) {
        mFileType = settings.get(0);
        mCommSetting = settings.get(1);
        this.mAccessRights = new HexString(settings.sliceOffLen(2, 2));
    }

    DesfireFileSettings(byte fileType, byte commSetting, ImmutableByteArray accessRights) {
        this.mFileType = fileType;
        this.mCommSetting = commSetting;
        this.mAccessRights = new HexString(accessRights);
    }

    public static DesfireFileSettings create(ImmutableByteArray data) throws Exception {
        byte fileType = data.get(0);

        if (fileType == STANDARD_DATA_FILE || fileType == BACKUP_DATA_FILE)
            return new StandardDesfireFileSettings(data);
        else if (fileType == LINEAR_RECORD_FILE || fileType == CYCLIC_RECORD_FILE)
            return new RecordDesfireFileSettings(data);
        else if (fileType == VALUE_FILE)
            return new ValueDesfireFileSettings(data);
        else
            throw new Exception("Unknown file type: " + Integer.toHexString(fileType));
    }

    public static DesfireFileSettings create(byte[] data) throws Exception {
        return create(ImmutableByteArray.Companion.fromByteArray(data));
    }

    public byte getFileType() {
        return mFileType;
    }

    public byte getCommSetting() {
        return mCommSetting;
    }

    public HexString getAccessRights() {
        return mAccessRights;
    }

    public @StringRes int getFileTypeString() {
        switch (mFileType) {
            case STANDARD_DATA_FILE:
                return R.string.desfire_standard_file;
            case BACKUP_DATA_FILE:
                return R.string.desfire_backup_file;
            case VALUE_FILE:
                return R.string.desfire_value_file;
            case LINEAR_RECORD_FILE:
                return R.string.desfire_linear_record;
            case CYCLIC_RECORD_FILE:
                return R.string.desfire_cyclic_record;
            default:
                return R.string.desfire_unknown_file;
        }
    }

    public abstract String getSubtitle();
}
