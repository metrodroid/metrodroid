package com.codebutler.farebot.card.desfire.settings;

import com.codebutler.farebot.card.desfire.settings.DesfireFileSettings;

import org.simpleframework.xml.Root;

@Root(name="settings")
public class UnsupportedDesfireFileSettings extends DesfireFileSettings {
    private UnsupportedDesfireFileSettings() { /* For XML Serializer */ }

    public UnsupportedDesfireFileSettings(byte fileType) {
        super(fileType, Byte.MIN_VALUE, new byte[0]);
    }
}
