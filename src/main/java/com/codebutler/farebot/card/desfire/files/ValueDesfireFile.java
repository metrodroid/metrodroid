/*
 * ValueDesfireFile.java
 *
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
package com.codebutler.farebot.card.desfire.files;

import com.codebutler.farebot.card.desfire.settings.DesfireFileSettings;
import com.codebutler.farebot.util.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Root;


/**
 * Represents a value file in Desfire
 */
@Root(name="file")
public class ValueDesfireFile extends DesfireFile {
    private int mValue;

    private ValueDesfireFile() { /* For XML Serializer */ }

    ValueDesfireFile(int fileId, DesfireFileSettings fileSettings, byte[] fileData) {
        super(fileId, fileSettings, fileData);

        byte[] myData = ArrayUtils.clone(fileData);
        ArrayUtils.reverse(myData);
        mValue = Utils.byteArrayToInt(myData);

    }

    public int getValue() { return mValue; }

}

