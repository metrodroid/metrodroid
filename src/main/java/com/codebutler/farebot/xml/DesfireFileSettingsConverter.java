package com.codebutler.farebot.xml;

import com.codebutler.farebot.card.desfire.settings.DesfireFileSettings;
import com.codebutler.farebot.card.desfire.settings.RecordDesfireFileSettings;
import com.codebutler.farebot.card.desfire.settings.StandardDesfireFileSettings;
import com.codebutler.farebot.card.desfire.settings.UnsupportedDesfireFileSettings;
import com.codebutler.farebot.card.desfire.settings.ValueDesfireFileSettings;
import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class DesfireFileSettingsConverter implements Converter<DesfireFileSettings> {
    @Override public DesfireFileSettings read(InputNode source) throws Exception {
        byte fileType = -1;
        int fileSize = -1;
        byte commSetting = -1;
        byte[] accessRights = new byte[0];
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
                        accessRights = Utils.hexStringToByteArray(value);
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

    @Override public void write(OutputNode node, DesfireFileSettings value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
