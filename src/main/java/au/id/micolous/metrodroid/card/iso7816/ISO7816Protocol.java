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
 * This is used by ISO7816 and CEPAS cards, as well as most credit cards.
 * <p>
 * References:
 * - EMV 4.3 Book 1 (s9, s11)
 * - https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit
 */
public class ISO7816Protocol {
    /**
     * If true, this turns on debug logs that show ISO7816 communication.
     */
    private static final boolean ENABLE_TRACING = false;

    private static final String TAG = ISO7816Protocol.class.getName();
    private static final byte CLASS_ISO7816 = (byte) 0x00;
    public static final byte CLASS_80 = (byte) 0x80;
    public static final byte CLASS_90 = (byte) 0x90;

    private static final byte INSTRUCTION_ISO7816_SELECT = (byte) 0xA4;
    private static final byte INSTRUCTION_ISO7816_READ_RECORD = (byte) 0xB2;

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
    public byte[] sendRequest(byte cla, byte ins, byte p1, byte p2, byte length, byte... parameters) throws IOException, CalypsoException {
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
                case (byte) 0x6A:
                    switch (sw2) {
                        case (byte) 0x82: // File not found
                            throw new FileNotFoundException();
                        case (byte) 0x83: // Record not found
                            throw new EOFException();
                    }
            }

            // we get error?
            throw new CalypsoException("Got unknown result: " + Utils.getHexString(recvBuffer, recvBuffer.length - 2, 2));
        }

        return Utils.byteArraySlice(recvBuffer, 0, recvBuffer.length - 2);
    }

    public byte[] selectApplication(byte[] application, boolean nextOccurrence) throws IOException {
        byte[] reply = null;
        Log.d(TAG, "Select application " + application);
        // Select an application by file name
        try {
            reply = sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_SELECT,
                    (byte) 0x04 /* byName */, nextOccurrence ? (byte) 0x02 : (byte) 0x00, (byte) 0,
                    application);
        } catch (CalypsoException | FileNotFoundException e) {
            Log.e(TAG, "couldn't select application", e);
            return null;
        }
        return reply;
    }

    public void unselectFile() throws IOException {
        Log.d(TAG, "Unselect file");
        try {
            sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_SELECT,
                    (byte) 0, (byte) 0, (byte) 0);
        } catch (CalypsoException e) {
            Log.e(TAG, "couldn't unselect file", e);
        }
    }

    public void selectFile(int fileId) throws IOException {
        byte[] file = Utils.integerToByteArray(fileId, 2);
        Log.d(TAG, "Select file " + Utils.getHexString(file));
        try {
            sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_SELECT,
                    (byte) 0, (byte) 0, (byte) 0,
                    file);
        } catch (CalypsoException e) {
            Log.e(TAG, "couldn't select file", e);
        }
    }

    public byte[] readRecord(byte recordNumber, byte length) throws IOException {
        byte[] ret;
        Log.d(TAG, "Read record " + recordNumber);
        try {
            ret = sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_READ_RECORD,
                    recordNumber, (byte) 0x4 /* p1 is record number */, length);



            return ret;
        } catch (CalypsoException e) {
            Log.e(TAG, "couldn't read record", e);
            return null;
        }

    }

    public class CalypsoException extends Exception {
        CalypsoException(String s) {
            super(s);
        }

        CalypsoException() {
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
