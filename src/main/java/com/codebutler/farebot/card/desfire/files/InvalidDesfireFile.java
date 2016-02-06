package com.codebutler.farebot.card.desfire.files;

import com.codebutler.farebot.card.desfire.settings.DesfireFileSettings;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name="file")
public class InvalidDesfireFile extends DesfireFile {
    @Element(name="error") private String mErrorMessage;

    protected InvalidDesfireFile() { /* For XML Serializer */ }

    public InvalidDesfireFile(int fileId, String errorMessage, DesfireFileSettings settings) {
        super(fileId, settings, new byte[0]);
        mErrorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    @Override public byte[] getData() {
        throw new IllegalStateException(String.format("Invalid file: %s", mErrorMessage));
    }
}
