/*
 * ISO7816Application.java
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
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Arrays;
import java.util.List;

import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.xml.HexString;

/**
 * Represents an application (DF) on an ISO7816 card.
 */
@Root(name = "application")
public class ISO7816Application {
    @Attribute(name = "name")
    private HexString mName;
    @ElementList(name = "files", required = false, empty = false)
    private List<ISO7816File> mFiles;

    ISO7816Application() { /* For XML Serializer */ }

    ISO7816Application(byte[] name, @Nullable List<ISO7816File> files) {
        mName = new HexString(name);
        mFiles = files;
    }

    public byte[] getName() {
        return mName.getData();
    }

    public List<ISO7816File> getFiles() {
        return mFiles;
    }

    /**
     * Gets a file for a given ID.
     * @param fileId File ID to retrieve.
     * @return {@see ISO7816File} with that index, or null if not present.
     */
    public ISO7816File getFile(byte[] fileId) {
        for (ISO7816File file : mFiles) {
            if (Arrays.equals(file.getFileName(), fileId)) {
                return file;
            }
        }

        return null;
    }
}