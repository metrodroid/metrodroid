/*
 * FelicaProtocol.java
 *
 * Copyright 2011 Kazzz
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc
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
 *
 * This code was imported from nfc-felica-lib[0], formerly licensed under
 * Apache License v2.0. The Apache Software Foundation considers the license
 * to be compatible with the GPLv3+[1].
 *
 * As the upstream library[0] appears to be no longer maintained, and we're not
 * aware of other users of Metrodroid's fork[2] of the library, this has been
 * pulled into Metrodroid proper[3]. It has been relicensed as GPLv3+ to be
 * consistent with the remainder of the project.
 *
 * [0]: https://github.com/Kazzz/nfc-felica-lib
 * [1]: https://www.apache.org/licenses/GPL-compatibility.html
 * [2]: https://github.com/metrodroid/nfc-felica-lib
 * [3]: https://github.com/micolous/metrodroid/pull/255
 */
package au.id.micolous.metrodroid.card.felica;

import android.nfc.TagLostException;
import android.nfc.tech.NfcF;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;

import au.id.micolous.metrodroid.util.ImmutableByteArray;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Protocol implementation for FeliCa and FeliCa Lite.
 *
 * This is adapted from nfc-felica-lib, and is still a work in progress to refactor it to be more
 * like ISO7816Protocol, and translate all the documentation into English.
 *
 * Note: This doesn't implement any of the write commands.
 *
 * FeliCa Card User's Manual, Excerpted Edition:
 *   https://www.sony.net/Products/felica/business/tech-support/data/card_usersmanual_2.02.pdf
 */

@SuppressWarnings("DuplicateThrows")
public final class FelicaProtocol {
    // CARD COMMANDS
    // Polling (s4.4.2)
    private static final byte COMMAND_POLLING = 0x00;
    private static final byte RESPONSE_POLLING = 0x01;

    // Request Service (s4.4.3)
    private static final byte COMMAND_REQUEST_SERVICE = 0x02;
    private static final byte RESPONSE_REQUEST_SERVICE = 0x03;

    // Request Response (s4.4.4)
    private static final byte COMMAND_REQUEST_RESPONSE = 0x04;
    private static final byte RESPONSE_REQUEST_RESPONSE = 0x05;

    // Read without encryption (s4.4.5)
    private static final byte COMMAND_READ_WO_ENCRYPTION = 0x06;
    private static final byte RESPONSE_READ_WO_ENCRYPTION = 0x07;

    // Write without encryption (s4.4.6)
    private static final byte COMMAND_WRITE_WO_ENCRYPTION = 0x08;
    private static final byte RESPONSE_WRITE_WO_ENCRYPTION = 0x09;

    // Search service code (s4.4.7, not documented publicly)
    private static final byte COMMAND_SEARCH_SERVICECODE = 0x0a;
    private static final byte RESPONSE_SEARCH_SERVICECODE = 0x0b;

    // Request system code (s4.4.8)
    private static final byte COMMAND_REQUEST_SYSTEMCODE = 0x0c;
    private static final byte RESPONSE_REQUEST_SYSTEMCODE = 0x0d;

    // Authentication 1 (s4.4.9, not documented publicly)
    private static final byte COMMAND_AUTHENTICATION1 = 0x10;
    private static final byte RESPONSE_AUTHENTICATION1 = 0x11;

    // Authentication 2 (s4.4.10, not documented publicly)
    private static final byte COMMAND_AUTHENTICATION2 = 0x12;
    private static final byte RESPONSE_AUTHENTICATION2 = 0x13;

    // Authenticated Read (s4.4.11, not documented publicly)
    private static final byte COMMAND_READ = 0x14;
    private static final byte RESPONSE_READ = 0x15;

    // Authenticated Write (s4.4.12, not documented publicly)
    private static final byte COMMAND_WRITE = 0x16;
    private static final byte RESPONSE_WRITE = 0x17;

    // SYSTEM CODES
    // Wildcard, matches any system code.
    public static final int SYSTEMCODE_ANY = 0xffff;
    // FeliCa Lite
    public static final int SYSTEMCODE_FELICA_LITE = 0x88b4;
    // NDEF (NFC Data Exchange Format)
    public static final int SYSTEMCODE_NDEF = 0x4000;
    // Common Area (FeliCa Networks, Inc), used by IC (Suica) and Edy
    public static final int SYSTEMCODE_COMMON = 0xfe00;

    // SERVICE CODES
    // FeliCa Lite, read-only mode
    public static final int SERVICE_FELICA_LITE_READONLY = 0x0b00;
    // FeliCa Lite, read-write mode
    public static final int SERVICE_FELICA_LITE_READWRITE = 0x0900;

    /**
     * If true, this turns on debug logs that show FeliCa communication.
     */
    private static final boolean ENABLE_TRACING = true;
    private static final String TAG = FelicaProtocol.class.getSimpleName();

    /** Connection to the FeliCa card */
    @NotNull
    private final NfcF mTagTech;

    /** IDm (Manufacturing ID) for the card */
    @Nullable
    private ImmutableByteArray idm;

    /** PMm (Manufacturing Parameters) for the card */
    @Nullable
    private ImmutableByteArray pmm;

    // TODO: Wrap in CardTransceiver
    public FelicaProtocol(@NotNull NfcF tagTech) {
        mTagTech = tagTech;
    }

    /**
     * Sends a command to the FeliCa card.
     *
     * This automatically appends the "size" to the start of the message, and automatically removes
     * the size from the response.
     *
     * @param commandCode Command code to send
     * @param idm IDm to add to command, or null to not add an IDm.
     * @param data Command parameters
     * @return Response from the card, or null.
     * @throws TagLostException If the tag moves out of the NFC field
     * @throws IOException On communications errors
     */
    @Nullable
    private ImmutableByteArray sendRequest(
            final byte commandCode, @Nullable final ImmutableByteArray idm, final byte... data)
            throws TagLostException, IOException {
        if ((commandCode & 0x01) != 0) {
            throw new IllegalArgumentException("commandCode must be even");
        }

        int length = data.length + 2;
        if (idm != null) {
            length += idm.size();
        }

        byte[] sendBuffer = new byte[length];
        sendBuffer[0] = (byte)length;
        sendBuffer[1] = commandCode;

        if (idm != null) {
            idm.copyInto(sendBuffer, 2, 0, idm.size());
            System.arraycopy(data, 0, sendBuffer, idm.size() + 2, data.length);
        } else {
            System.arraycopy(data, 0, sendBuffer, 2, data.length);
        }

        if (ENABLE_TRACING) {
            Log.d(TAG, ">>> " + Utils.getHexString(sendBuffer));
        }

        byte[] recvBuffer = mTagTech.transceive(sendBuffer);
        if (recvBuffer == null) {
            if (ENABLE_TRACING) {
                Log.d(TAG, "<<< (null)");
            }

            return null;
        }

        if (ENABLE_TRACING) {
            Log.d(TAG, "<<< " + Utils.getHexString(recvBuffer));
        }

        // Check command code
        if (commandCode + 1 != recvBuffer[1]) {
            throw new IOException("response had unexpected command code");
        }

        // Automatically strip off the length prefix before returning it.
        return ImmutableByteArray.Companion.fromByteArray(recvBuffer).sliceOffLen(
                1, recvBuffer.length - 1);
    }

    /**
     * Polls for a card with the given system code.
     *
     * Automatically sets the IDm and PMm of the card based on the response.
     *
     * This does not pass the IDm as a parameter, and may select <em>any</em> nearby tag.  If no
     * IDm or PMm is given in the response, this will <em>unset</em> these values.
     *
     * @param systemCode System code to search for, or SYSTEMCODE_ANY to scan for any card.
     * @return The response from the card, including the IDm and PMm
     * @throws IOException On communication errors
     * @throws TagLostException If the tag moves out of the field, or there is no response
     */
    public ImmutableByteArray polling(int systemCode) throws IOException, TagLostException {
        ImmutableByteArray res = sendRequest(COMMAND_POLLING, null,
                (byte) (systemCode >> 8),   // System code (upper byte)
                (byte) (systemCode & 0xff), // System code (lower byte)
                (byte) 0x01,                // Request code (system code request)
                (byte) 0x07);               // Maximum number of time slots to respond

        if (res == null) {
            return null;
        }

        if (res.size() >= 9) {
            idm = res.sliceOffLen(1, 8);
        } else {
            idm = null;
        }

        if (res.size() >= 17) {
            pmm = res.sliceOffLen(9, 8);
        } else {
            pmm = null;
        }

        return res;
    }

    /**
     * Polls for the given system code, and returns the IDm of the responding card.
     *
     * @param systemCode System code to search for, or SYSTEMCODE_ANY to scan for any card.
     * @return The responding card's IDm.
     * @throws IOException On communication errors
     * @throws TagLostException If the tag moves out of the field, or there is no response
     */
    @Nullable
    public ImmutableByteArray pollingAndGetIDm(int systemCode)
            throws IOException, TagLostException {
        polling(systemCode);
        return idm;
    }

    /**
     * Gets the IDm (Manufacture Identifier).  This is the serial number of a FeliCa card.
     */
    @Nullable
    public ImmutableByteArray getIdm() {
        return idm;
    }

    /**
     * Gets the PMm (Manufacturing Parameters).
     */
    @Nullable
    public ImmutableByteArray getPmm() {
        return pmm;
    }

    /**
     * Gets a list of system codes supported by the card.
     *
     * @throws TagLostException if the tag went out of the field
     */
    @NotNull
    public int[] getSystemCodeList() throws IOException, TagLostException {
        //request systemCode
        ImmutableByteArray retBytes = sendRequest(COMMAND_REQUEST_SYSTEMCODE, idm);

        if (retBytes == null) {
            // No system codes were received from the card.
            return new int[0];
        }

        int count = retBytes.byteArrayToInt(9, 1);

        if (10 + (count * 2) > retBytes.size()) {
            Log.w(TAG, "Got too few bytes from FeliCa for system code list, truncating...");
            count = (retBytes.size() - 10) / 2;
        }

        int[] ret = new int[count];
        for (int i = 0; i < count; i++) {
            ret[i] = retBytes.byteArrayToInt(10 + (i * 2), 2);
        }

        return ret;
    }


    @NotNull
    private ImmutableByteArray searchServiceCode(int index) throws IOException, TagLostException {
        ImmutableByteArray res = sendRequest(COMMAND_SEARCH_SERVICECODE, idm,
                (byte) (index & 0xff), // little endian
                (byte) (index << 8));

        if (res == null || res.size() <= 0) {
            return ImmutableByteArray.Companion.empty();
        }

        return res.sliceOffLen(9, res.size()-9);
    }

    /**
     * Gets a list of service codes supported by the card.
     *
     * The service codes in "corrected" byte order -- SEARCH_SERVICECODE returns service codes in
     * little endian, and the read/write commands take in service codes in little endian.
     */
    public int[] getServiceCodeList() throws IOException, TagLostException {
        int index = 1; // 0番目は root areaなので1オリジンで開始する
        ArrayList<Integer> serviceCodeList = new ArrayList<>();
        while (true) {
            ImmutableByteArray bytes = searchServiceCode(index); // 1件1件 通信して聞き出します。
            if (bytes.size() != 2 && bytes.size() != 4)
                break; // 2 or 4 バイトじゃない場合は、とりあえず終了しておきます。正しい判定ではないかもしれません。

            if (bytes.size() == 2) { // 2バイトは ServiceCode として扱っています。
                // Note: we handle the service code internally as if it were little endian, as
                // "read without encryption" takes the service code parameter in little endian
                int code = bytes.byteArrayToIntReversed();
                if (code == 0xffff) break; // FFFF が終了コードのようです

                serviceCodeList.add(code);
            }

            index++;

            if (index > 0xffff) {
                // Invalid service code index
                break;
            }
        }
        return ArrayUtils.toPrimitive(serviceCodeList.toArray(new Integer[0]));
    }

    /**
     * Reads a given service code without encryption.
     *
     * @param serviceCode The service code to read. This is converted to little endian before being
     *                    transmitted to the card.
     * @param addr        Block offset to read from the cord.
     * @throws TagLostException if the tag went out of the field
     */
    public ImmutableByteArray readWithoutEncryption(
            int serviceCode, byte addr) throws IOException, TagLostException {
        // read without encryption
        ImmutableByteArray resp = sendRequest(COMMAND_READ_WO_ENCRYPTION, idm,
                (byte) 0x01,                 // Number of service codes
                (byte) (serviceCode & 0xff), // Service code (lower byte)
                (byte) (serviceCode >> 8),   // Service code (upper byte)
                (byte) 0x01,                 // Number of blocks to read
                (byte) 0x80,                 // Block (upper byte, always 0x80)
                addr);                       // Block (lower byte)

        if (resp == null) return null;

        if (resp.get(9) != 0) {
            // Status flag 1
            return null;
        }

        return resp.sliceOffLen(12, 16);
    }
}
