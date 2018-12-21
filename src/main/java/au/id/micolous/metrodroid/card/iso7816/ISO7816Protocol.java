/*
 * ISO7816Protocol.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

import au.id.micolous.metrodroid.card.CardTransceiver;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

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
 * - https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit
 */
public class ISO7816Protocol {
    /**
     * If true, this turns on debug logs that show ISO7816 communication.
     */
    private static final boolean ENABLE_TRACING = true;

    private static final String TAG = ISO7816Protocol.class.getSimpleName();
    @VisibleForTesting
    public static final byte CLASS_ISO7816 = (byte) 0x00;
    public static final byte CLASS_80 = (byte) 0x80;
    public static final byte CLASS_90 = (byte) 0x90;

    @VisibleForTesting
    public static final byte INSTRUCTION_ISO7816_SELECT = (byte) 0xA4;
    @VisibleForTesting
    public static final byte INSTRUCTION_ISO7816_READ_BINARY = (byte) 0xB0;
    @VisibleForTesting
    public static final byte INSTRUCTION_ISO7816_READ_RECORD = (byte) 0xB2;
    @VisibleForTesting
    public static final byte ERROR_COMMAND_NOT_ALLOWED = (byte) 0x69;
    @VisibleForTesting
    public static final byte ERROR_WRONG_PARAMETERS = (byte) 0x6A;
    @VisibleForTesting
    public static final byte CNA_NO_CURRENT_EF = (byte) 0x86;
    @VisibleForTesting
    public static final byte WP_FILE_NOT_FOUND = (byte) 0x82;
    @VisibleForTesting
    public static final byte WP_RECORD_NOT_FOUND = (byte) 0x83;
    @VisibleForTesting
    public static final byte SELECT_BY_NAME = (byte) 0x04;
    @VisibleForTesting
    public static final byte STATUS_OK = (byte) 0x90;


    private final CardTransceiver mTagTech;

    public ISO7816Protocol(CardTransceiver tagTech) {
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
    @NonNull
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
     * @throws FileNotFoundException If a requested file can not be found
     * @throws EOFException If a requested record can not be found
     * @throws IllegalStateException If an invalid command is issued
     * @throws IOException If there is a communication error
     * @throws ISO7816Exception If there is an unhandled error code
     * @return A wrapped command.
     */
    @NonNull
    public byte[] sendRequest(byte cla, byte ins, byte p1, byte p2, byte length, byte... parameters) throws IOException, ISO7816Exception {
        byte[] sendBuffer = wrapMessage(cla, ins, p1, p2, length, parameters);
        if (ENABLE_TRACING) {
            Log.d(TAG, ">>> " + Utils.getHexString(sendBuffer));
        }
        byte[] recvBuffer = mTagTech.transceive(sendBuffer);
        if (ENABLE_TRACING) {
            Log.d(TAG, "<<< " + Utils.getHexString(recvBuffer));
        }

        if (recvBuffer.length == 1) {
            // Android HCE does this for some commands ?
            throw new ISO7816Exception("Got 1-byte result: " + Utils.getHexString(recvBuffer));
        }

        byte sw1 = recvBuffer[recvBuffer.length - 2];
        byte sw2 = recvBuffer[recvBuffer.length - 1];

        if (sw1 != STATUS_OK) {
            switch (sw1) {
                case ERROR_COMMAND_NOT_ALLOWED: // Command not allowed
                    switch (sw2) {
                        case CNA_NO_CURRENT_EF: // Command not allowed (no current EF)
                            // Emitted by Android HCE when doing a CEPAS probe
                            throw new IllegalStateException();
                    }
                    break;

                case ERROR_WRONG_PARAMETERS: // Wrong Parameters P1 - P2
                    switch (sw2) {
                        case WP_FILE_NOT_FOUND: // File not found
                            throw new FileNotFoundException();
                        case WP_RECORD_NOT_FOUND: // Record not found
                            throw new EOFException();
                    }
                    break;
            }

            // we get error?
            throw new ISO7816Exception("Got unknown result: " + Utils.getHexString(recvBuffer, recvBuffer.length - 2, 2));
        }

        return Utils.byteArraySlice(recvBuffer, 0, recvBuffer.length - 2);
    }

    @NonNull
    public byte[] selectByName(@NonNull byte[] name, boolean nextOccurrence) throws IOException, ISO7816Exception {
        Log.d(TAG, "Select by name " + Utils.getHexString(name));
        // Select an application by file name
        return sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_SELECT,
                SELECT_BY_NAME, nextOccurrence ? (byte) 0x02 : (byte) 0x00, (byte) 0,
                    name);
    }

    public void unselectFile() throws IOException, ISO7816Exception {
        Log.d(TAG, "Unselect file");
        sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_SELECT,
                    (byte) 0, (byte) 0, (byte) 0);
    }

    public byte[] selectById(int fileId) throws IOException, ISO7816Exception {
        byte[] file = Utils.integerToByteArray(fileId, 2);
        Log.d(TAG, "Select file " + Utils.getHexString(file));
        return sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_SELECT,
                    (byte) 0, (byte) 0, (byte) 0,
                    file);
    }

    @Nullable
    public byte[] readRecord(byte recordNumber, byte length) throws IOException {
        byte[] ret;
        //noinspection StringConcatenation
        Log.d(TAG, "Read record " + recordNumber);
        try {
            ret = sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_READ_RECORD,
                    recordNumber, (byte) 0x4 /* p1 is record number */, length);

            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read record", e);
            return null;
        }
    }

    @Nullable
    public byte[] readBinary() throws IOException {
        byte[] ret;
        Log.d(TAG, "Read binary");
        try {
            ret = sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_READ_BINARY, (byte) 0, (byte) 0, (byte) 0);

            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read record", e);
            return null;
        }
    }

    @Nullable
    public byte[] selectByNameOrNull(@NonNull byte[] name) {
        try {
            return selectByName(name, false);
        } catch (ISO7816Exception | IOException e) {
            return null;
        }
    }

    @Nullable
    public byte[] readBinary(byte sfi) throws IOException {
        byte[] ret;
        Log.d(TAG, "Read binary");
        try {
            ret = sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_READ_BINARY, (byte) (0x80 | sfi), (byte) 0, (byte) 0);
            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read record", e);
            return null;
        }
    }

    @Nullable
    public byte[] readRecord(byte sfi, byte recordNumber, byte length) throws IOException {
        byte[] ret;
        //noinspection StringConcatenation
        Log.d(TAG, "Read record " + recordNumber);
        try {
            ret = sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_READ_RECORD,
                    recordNumber, (byte) ((sfi << 3) | 4) /* p1 is record number */, length);
            return ret;
        } catch (ISO7816Exception e) {
            Log.e(TAG, "couldn't read record", e);
            return null;
        }
    }
}
