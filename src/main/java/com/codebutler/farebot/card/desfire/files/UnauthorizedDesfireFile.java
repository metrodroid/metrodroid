package com.codebutler.farebot.card.desfire.files;

import com.codebutler.farebot.card.desfire.settings.DesfireFileSettings;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * Represents a DESFire file which could not be read due to
 * access control limits.
 */
@Root(name="file")
public class UnauthorizedDesfireFile extends InvalidDesfireFile {
    @Attribute(name="unauthorized") public static final boolean UNAUTHORIZED = true;

    private UnauthorizedDesfireFile() { /* For XML Serializer */ }

    public UnauthorizedDesfireFile(int fileId, String errorMessage, DesfireFileSettings settings) {
        super(fileId, errorMessage, settings);
    }

    @Override public byte[] getData() {
        throw new IllegalStateException(String.format("Unauthorized access to file: %s", getErrorMessage()));
    }
}
