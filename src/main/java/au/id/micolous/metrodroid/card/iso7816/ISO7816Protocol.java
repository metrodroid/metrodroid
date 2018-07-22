/*
 * ISO7816Protocol.java
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

import android.nfc.tech.IsoDep;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

import au.id.micolous.metrodroid.util.Utils;

/**
 * Implements communication with cards that talk over ISO7816-4 APDUs.
 * <p>
 * Android doesn't contain useful classes for interfacing with these APDUs, so this class implements
 * basic parts of the specification. In particular, this only supports open communication with the
 * card, and doesn't support writing data.
 * <p>
 * This is used by Calypso and CEPAS cards, as well as most credit cards.
 * <p>
 * References:
 * - EMV 4.3 Book 1 (s9, s11)
 * - ISO/IEC 7816-4
 * - https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit
 */
public class ISO7816Protocol {
    /**
     * If true, this turns on debug logs that show ISO7816 communication.
     */
    private static final boolean ENABLE_TRACING = true;

    private static final String TAG = ISO7816Protocol.class.getSimpleName();

    // CLA
    private static final byte CLASS_ISO7816 = (byte) 0x00;

    // INS
    private static final byte INS_SELECT = (byte) 0xA4;
    private static final byte INS_READ_BINARY = (byte) 0xB0;
    private static final byte INS_READ_BINARY1 = (byte) 0xB1;
    private static final byte INS_READ_RECORD = (byte) 0xB2;
    private static final byte INS_GET_DATA = (byte) 0xCA;
    private static final byte INS_GET_DATA1 = (byte) 0xCB;

    // Error conditions
    private static final byte ERROR_WRONG_PARAMETERS = (byte) 0x6A;
    private static final byte ERROR_WRONG_PARAMETERS_6B = (byte) 0x6B;

    // Specific to ERROR_WRONG_PARAMETERS:
    private static final byte ERROR_6A_FILE_NOT_FOUND = (byte) 0x82;
    private static final byte ERROR_6A_RECORD_NOT_FOUND = (byte) 0x83;



    private IsoDep mTagTech;

    public ISO7816Protocol(IsoDep tagTech) {
        mTagTech = tagTech;
    }

    /**
     * Creates a C-APDU. (EMV 4.3 Book 1 s9.4.1)
     * <p>
     * This always sends with Le (expected return length) of 0 (=256 bytes).
     *
     * @param cla        Instruction class, may be any value but 0xFF.
     * @param ins        Instruction code within the instruction class.
     * @param p1         Reference byte completing the INS.
     * @param p2         Reference byte completing the INS.
     * @param length     Length of the expected return value, or 0 for no limit.
     * @param parameters Additional data to be send in a command.
     * @return A wrapped command.
     */
    private byte[] wrapMessage(byte cla, byte ins, byte p1, byte p2, byte length, byte... parameters) {
        byte[] output = new byte[5 + (parameters.length == 0 ? 0 : 1 + parameters.length)];
        output[0] = cla;
        output[1] = ins;
        output[2] = p1;
        output[3] = p2;

        if (parameters.length > 0) {
            output[4] = (byte) parameters.length;
            System.arraycopy(parameters, 0, output, 5, parameters.length);
        }

        output[output.length - 1] = length;
        return output;
    }

    /**
     * Sends a command to the card and checks the response.
     *
     * @param cla        Instruction class, may be any value but 0xFF.
     * @param ins        Instruction code within the instruction class.
     * @param p1         Reference byte completing the INS.
     * @param p2         Reference byte completing the INS.
     * @param length     Length of the expected return value, or 0 for no limit.
     * @param parameters Additional data to be send in a command.
     * @return A wrapped command.
     */
    private byte[] sendRequest(byte cla, byte ins, byte p1, byte p2, byte length, byte... parameters)
            throws IOException, ISO7816Exception, IllegalArgumentException {
        byte[] sendBuffer = wrapMessage(cla, ins, p1, p2, length, parameters);
        if (ENABLE_TRACING) {
            Log.d(TAG, ">>> " + Utils.getHexString(sendBuffer));
        }
        byte[] recvBuffer = mTagTech.transceive(sendBuffer);
        if (ENABLE_TRACING) {
            Log.d(TAG, "<<< " + Utils.getHexString(recvBuffer));
        }

        byte sw1 = recvBuffer[recvBuffer.length - 2];
        byte sw2 = recvBuffer[recvBuffer.length - 1];

        if (sw1 != (byte) 0x90) {
            switch (sw1) {
                case ERROR_WRONG_PARAMETERS:
                    switch (sw2) {
                        case ERROR_6A_FILE_NOT_FOUND:
                            throw new FileNotFoundException();
                        case ERROR_6A_RECORD_NOT_FOUND:
                            throw new EOFException();
                    }
                    break;

                case ERROR_WRONG_PARAMETERS_6B:
                    throw new IllegalArgumentException("Wrong parameters: " + Utils.getHexString(recvBuffer, recvBuffer.length - 2, 2));
            }

            // we get error?
            throw new ISO7816Exception("Got unknown result: " + Utils.getHexString(recvBuffer, recvBuffer.length - 2, 2));
        }

        return Utils.byteArraySlice(recvBuffer, 0, recvBuffer.length - 2);
    }

    public byte[] selectApplication() throws IOException {
        return selectApplication(false);
    }

    public byte[] selectApplication(boolean nextOccurrence) throws IOException {
        Log.d(TAG, "Select application (any)");
        return selectApplication(new byte[0], nextOccurrence);
    }

    public byte[] selectApplication(String application) throws IOException {
        return selectApplication(application, false);
    }

    public byte[] selectApplication(String application, boolean nextOccurrence) throws IOException {
        Log.d(TAG, "Select application " + application);
        return selectApplication(Utils.stringToByteArray(application), nextOccurrence);
    }

    public byte[] selectApplication(byte[] application) throws IOException {
        return selectApplication(application, false);
    }

    public byte[] selectApplication(byte[] application, boolean nextOccurrence) throws IOException {
        Log.d(TAG, "Select application " + Utils.getHexString(application));
        // Select an application by file name
        try {
            return sendRequest(CLASS_ISO7816, INS_SELECT,
                    (byte) 0x04 /* byName */, nextOccurrence ? (byte) 0x02 : (byte) 0x00, (byte) 0,
                    application);
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't select application", e);
            return null;
        }
    }

    public void unselectFile() throws IOException {
        Log.d(TAG, "Unselect file");
        try {
            sendRequest(CLASS_ISO7816, INS_SELECT,
                    (byte) 0, (byte) 0, (byte) 0);
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't unselect file", e);
        }
    }

    public void selectFile(int fileId) throws IOException {
        byte[] file = Utils.integerToByteArray(fileId, 2);
        Log.d(TAG, "Select file " + Utils.getHexString(file));
        try {
            sendRequest(CLASS_ISO7816, INS_SELECT,
                    (byte) 0, (byte) 0, (byte) 0,
                    file);
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't select file", e);
        }
    }

    public void selectFileByPath(long pathId, int length) throws IOException {
        byte[] path = Utils.integerToByteArray(pathId, length);
        Log.d(TAG, "Select file by path " + Utils.getHexString(path));
        try {
            sendRequest(CLASS_ISO7816, INS_SELECT,
                    (byte) 0x8 /* select by path from mf */, (byte) 0, (byte) 0,
                    path);
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't select file", e);
        }
    }


    public byte[] walkFile(boolean nextFile) throws IOException {
        Log.d(TAG, "Select next file");
        try {
            return sendRequest(CLASS_ISO7816, INS_SELECT,
                    (byte) 0 /* mf || df || ef */,
                    (nextFile ? (byte) 0x02 : (byte) 0x00),
                    (byte) 0);
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't select next file", e);
            return null;
        }
    }

    public byte[] readRecord(byte recordNumber, byte length) throws IOException {
        byte[] ret;
        Log.d(TAG, "Read record " + recordNumber);
        try {
            ret = sendRequest(CLASS_ISO7816, INS_READ_RECORD,
                    recordNumber, (byte) 0x4 /* p1 is record number */, length);
            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read record", e);
            return null;
        }

    }

    public byte[] getAnswerToReset() throws IOException {
        byte[] ret;
        Log.d(TAG, "Get answer to reset");
        try {
            ret = sendRequest(CLASS_ISO7816, INS_GET_DATA,
                    (byte) 0x5F, (byte) 0x51, (byte)0);

            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read answer to reset");
            return null;
        }
    }

    public byte[] getHistoricalBytes() throws IOException {
        byte[] ret;
        Log.d(TAG, "Get historical bytes");
        try {
            ret = sendRequest(CLASS_ISO7816, INS_GET_DATA,
                    (byte) 0x5F, (byte) 0x52, (byte)0);

            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read historical bytes");
            return null;
        }
    }

    public byte[] getInitialDataString() throws IOException {
        byte[] ret;
        Log.d(TAG, "Get initial data string");
        try {
            ret = sendRequest(CLASS_ISO7816, INS_READ_BINARY,
                    (byte) 0, (byte) 0, (byte)0);

            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read initial data string");
            return null;
        }
    }

    public byte[] readBinary(int efId) throws IOException {
        byte[] ret;
        byte[] file = Utils.integerToByteArray(efId, 2);
        Log.d(TAG, "Read binary " + Utils.getHexString(file));
        try {
            ret = sendRequest(CLASS_ISO7816, INS_READ_BINARY1,
                    file[0], file[1], (byte)0);

            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read binary");
            return null;
        }

    }

    public byte[] readFile(int fileId) throws IOException {
        byte[] ret;
        Log.d(TAG, "Get file " + fileId);
        try {
            ret = sendRequest(CLASS_ISO7816, INS_GET_DATA1,
                    (byte) ((fileId >> 8) & 0xFF),
                    (byte) (fileId & 0xFF),
                    (byte)0,
                    /* empty tag list */ (byte)0x5C, (byte)0
            );

            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read file");
            return null;
        }
    }

    public byte[] readEfDir() throws IOException {

        return readRecord((byte)0, (byte)0);
    }

    public byte[] readEfAtr() throws IOException {
        return readFile(0x2f01);
    }

    public class ISO7816Exception extends Exception {
        ISO7816Exception(String s) {
            super(s);
        }

        ISO7816Exception() {
        }
    }

    class ReadLengthFieldResult {
        /** value of the length field */
        int length;
        /** the number of bytes it took to encode this length value */
        int bytesConsumed;

        ReadLengthFieldResult(int length, int bytesConsumed) {
            this.length = length;
            this.bytesConsumed = bytesConsumed;
        }
    }

    /**
     * Decodes a BER-TLV length delimiter (X.690 ASN.1).
     *
     * This implements the limited subset for ISO 7816 (where it may only consume up to 5 bytes).
     *
     * A worked example of the encoding is given at:
     * https://en.wikipedia.org/wiki/X.690#Definite_form
     * @param buf Buffer to read
     * @param offset Offset to start reading from
     * @return A ReadLengthFieldResult if the value is valid, or NULL if the value is invalid.
     */
    @Nullable
    private ReadLengthFieldResult readLengthField(byte[] buf, int offset) {
        int bytesConsumed = buf[offset] & 0xff;

        if (bytesConsumed <= 0x7f) {
            return new ReadLengthFieldResult(bytesConsumed, 1);
        }

        // Chop off the top bit
        bytesConsumed &= 0x7f;

        if (bytesConsumed == 0 || bytesConsumed > 4) {
            // length is invalid
            return null;
        }

        int length = 0;
        for (int x=1; x<=bytesConsumed; x++) {
            length = (length << 8) | (buf[offset + 1] & 0xff);
        }

        return new ReadLengthFieldResult(length, bytesConsumed);
    }

}
