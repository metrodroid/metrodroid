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
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

@Root(name = "file")
public class DesfireFile {
    @Attribute(name = "id")
    private int mId;
    @Element(name = "settings", required = false)
    private DesfireFileSettings mSettings;
    @Element(name = "data", required = false)
    private Base64String mData;

    DesfireFile() { /* For XML Serializer */ }

    DesfireFile(int fileId, DesfireFileSettings fileSettings, ImmutableByteArray fileData) {
        mId = fileId;
        mSettings = fileSettings;
        mData = new Base64String(fileData);
    }

    public static DesfireFile create(int fileId, DesfireFileSettings fileSettings, ImmutableByteArray fileData) {
        if (fileSettings instanceof RecordDesfireFileSettings) {
            return new RecordDesfireFile(fileId, fileSettings, fileData);
        } else if (fileSettings instanceof ValueDesfireFileSettings) {
            return new ValueDesfireFile(fileId, fileSettings, fileData);
        } else {
            return new DesfireFile(fileId, fileSettings, fileData);
        }
    }

    public static DesfireFile create(int fileId, DesfireFileSettings fileSettings, byte[] fileDataRaw) {
        return create(fileId, fileSettings, ImmutableByteArray.Companion.fromByteArray(fileDataRaw));
    }

    public DesfireFileSettings getFileSettings() {
        return mSettings;
    }

    public final int getId() {
        return mId;
    }

    public ImmutableByteArray getData() {
        return mData;
    }

    public ListItem getRawData() {
        return new ListItemRecursive(Utils.localizeString(R.string.file_title_format,
                Utils.intToHex(getId())),
                getFileSettings().getSubtitle(),
                Collections.singletonList(new ListItem(null, getData().toHexDump())));
    }
}
