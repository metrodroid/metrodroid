/*
 * DesfireFileSettingnsConverter.java
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
package au.id.micolous.metrodroid.xml;

import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.RecordDesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.StandardDesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.UnsupportedDesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.ValueDesfireFileSettings;
import au.id.micolous.metrodroid.util.ImmutableByteArray;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class DesfireFileSettingsConverter implements Converter<DesfireFileSettings> {
    @Override
    public DesfireFileSettings read(InputNode source) throws Exception {
        byte fileType = -1;
        int fileSize = -1;
        byte commSetting = -1;
        ImmutableByteArray accessRights = ImmutableByteArray.Companion.empty();
        int recordSize = -1;
        int maxRecords = -1;
        int curRecords = -1;
        int lowerLimit = -1;
        int upperLimit = -1;
        int limitedCreditValue = -1;
        boolean limitedCreditEnabled = false;

        while (true) {
            InputNode node = source.getNext();
            if (node == null) {
                break;
            }

            String value = node.getValue();

            if (value != null) {
                switch (node.getName()) {
                    case "filetype":
                        fileType = Byte.parseByte(value);
                        break;
                    case "filesize":
                        fileSize = Integer.parseInt(value);
                        break;
                    case "commsetting":
                        commSetting = Byte.parseByte(value);
                        break;
                    case "accessrights":
                        accessRights = ImmutableByteArray.Companion.fromHex(value);
                        break;

                    case "recordsize":
                        recordSize = Integer.parseInt(value);
                        break;
                    case "maxrecords":
                        maxRecords = Integer.parseInt(value);
                        break;
                    case "currecords":
                        curRecords = Integer.parseInt(value);
                        break;

                    case "min":
                        lowerLimit = Integer.parseInt(value);
                        break;
                    case "max":
                        upperLimit = Integer.parseInt(value);
                        break;
                    case "limitcredit":
                        limitedCreditValue = Integer.parseInt(value);
                        break;
                    case "limitcreditenabled":
                        limitedCreditEnabled = Boolean.parseBoolean(value);
                        break;

                }
            }
        }

        switch (fileType) {
            case DesfireFileSettings.STANDARD_DATA_FILE:
            case DesfireFileSettings.BACKUP_DATA_FILE:
                return new StandardDesfireFileSettings(fileType, commSetting, accessRights, fileSize);
            case DesfireFileSettings.LINEAR_RECORD_FILE:
            case DesfireFileSettings.CYCLIC_RECORD_FILE:
                return new RecordDesfireFileSettings(fileType, commSetting, accessRights, recordSize, maxRecords,
                        curRecords);
            case DesfireFileSettings.VALUE_FILE:
                return new ValueDesfireFileSettings(fileType, commSetting, accessRights, lowerLimit, upperLimit, limitedCreditValue, limitedCreditEnabled);
            default:
                return new UnsupportedDesfireFileSettings(fileType);
        }
    }

    @Override
    public void write(OutputNode node, DesfireFileSettings value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
