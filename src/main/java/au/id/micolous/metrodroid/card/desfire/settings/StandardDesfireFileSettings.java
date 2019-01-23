/*
 * StandardDesfireFileSettings.java
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
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

@Root(name = "settings")
public class StandardDesfireFileSettings extends DesfireFileSettings {
    @Element(name = "filesize")
    private int mFileSize;

    private StandardDesfireFileSettings() { /* For XML Serializer */ }

    StandardDesfireFileSettings(ImmutableByteArray buf) {
        super(buf);
        mFileSize = buf.byteArrayToIntReversed(4, 3);
    }

    public StandardDesfireFileSettings(byte fileType, byte commSetting, ImmutableByteArray accessRights, int fileSize) {
        super(fileType, commSetting, accessRights);
        this.mFileSize = fileSize;
    }

    public int getFileSize() {
        return mFileSize;
    }

    @Override
    public String getSubtitle() {
        return Localizer.INSTANCE.localizePlural(R.plurals.desfire_standard_format,
                getFileSize(),
                Localizer.INSTANCE.localizeString(getFileTypeString()),
                getFileSize());
    }
}
