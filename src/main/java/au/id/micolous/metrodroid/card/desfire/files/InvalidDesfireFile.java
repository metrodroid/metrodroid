/*
 * InvalidDesfireFile.java
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

import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.NumberUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

@Root(name = "file")
public class InvalidDesfireFile extends DesfireFile {
    @Element(name = "error")
    private String mErrorMessage;

    protected InvalidDesfireFile() { /* For XML Serializer */ }

    public InvalidDesfireFile(int fileId, String errorMessage, DesfireFileSettings settings) {
        super(fileId, settings, ImmutableByteArray.Companion.empty());
        mErrorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    @Override
    public ImmutableByteArray getData() {
        throw new IllegalStateException(String.format("Invalid file: %s", mErrorMessage));
    }

    @Override
    public ListItem getRawData() {
        return new ListItem(Localizer.INSTANCE.localizeString(R.string.invalid_file_title_format,
                NumberUtils.INSTANCE.intToHex(getId()),
                getErrorMessage()));
    }
}
