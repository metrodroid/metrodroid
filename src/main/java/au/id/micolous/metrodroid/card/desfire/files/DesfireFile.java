/*
 * DesfireFile.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.Collections;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.RecordDesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.ValueDesfireFileSettings;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

@Root(name = "file")
public class DesfireFile {
    @Attribute(name = "id")
    private int mId;
    @Nullable
    @Element(name = "settings", required = false)
    private DesfireFileSettings mSettings;
    @Nullable
    @Element(name = "data", required = false)
    private Base64String mData;

    DesfireFile() { /* For XML Serializer */ }

    DesfireFile(int fileId,
                @Nullable DesfireFileSettings fileSettings,
                @Nullable ImmutableByteArray fileData) {
        mId = fileId;
        mSettings = fileSettings;
        mData = fileData != null ? new Base64String(fileData) : null;
    }

    public static DesfireFile create(int fileId,
                                     @Nullable DesfireFileSettings fileSettings,
                                     @Nullable ImmutableByteArray fileData) {
        if (fileSettings instanceof RecordDesfireFileSettings) {
            return new RecordDesfireFile(fileId, fileSettings, fileData);
        } else if (fileSettings instanceof ValueDesfireFileSettings) {
            return new ValueDesfireFile(fileId, fileSettings, fileData);
        } else {
            return new DesfireFile(fileId, fileSettings, fileData);
        }
    }

    public static DesfireFile create(int fileId,
                                     @NotNull DesfireFileSettings fileSettings,
                                     @Nullable byte[] fileDataRaw) {

        return create(fileId,
                fileSettings,
                fileDataRaw != null ? ImmutableByteArray.Companion.fromByteArray(fileDataRaw) : null);
    }

    @Nullable
    public DesfireFileSettings getFileSettings() {
        return mSettings;
    }

    public final int getId() {
        return mId;
    }

    @Nullable
    public ImmutableByteArray getData() {
        return mData;
    }

    public ListItem getRawData() {
        final ImmutableByteArray data = getData();
        final DesfireFileSettings settings = getFileSettings();
        return new ListItemRecursive(
                Localizer.INSTANCE.localizeString(R.string.file_title_format,
                    NumberUtils.INSTANCE.intToHex(getId())),
                settings != null
                    ? settings.getSubtitle()
                    : null,
                data != null
                    ? Collections.singletonList(new ListItem(null, data.toHexDump()))
                    : null);
    }
}
