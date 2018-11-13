/*
 * DesfireProtocol.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.desfire;

import android.nfc.tech.IsoDep;
import android.support.annotation.NonNull;
import android.util.Log;

import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings;
import au.id.micolous.metrodroid.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.Locale;

/**
 * Implements communication with MIFARE DESFire cards.
 *
 * Android doesn't contain useful classes for interfacing with DESFire, so this is class implements
 * some very basic functionality to interface. In particular, this only supports open communication
 * with the card, and doesn't support writing data.
 *
 * Useful references:
 * https://github.com/nfc-tools/libfreefare/blob/master/libfreefare/mifare_desfire.c
 * https://github.com/jekkos/android-hce-desfire/blob/master/hceappletdesfire/src/main/java/net/jpeelaer/hce/desfire/DesfireApplet.java
 * https://github.com/jekkos/android-hce-desfire/blob/master/hceappletdesfire/src/main/java/net/jpeelaer/hce/desfire/DesFireInstruction.java
 * https://ridrix.wordpress.com/2009/09/19/mifare-desfire-communication-example/
 */
public class DesfireProtocol {
    private static final String TAG = "DesfireProtocol";

    // Commands
    static public final byte UNLOCK = (byte) 0x0A;
    static public final byte GET_MANUFACTURING_DATA = (byte) 0x60;
    static final byte GET_APPLICATION_DIRECTORY = (byte) 0x6A;
    static final byte GET_ADDITIONAL_FRAME = (byte) 0xAF;
    static final byte SELECT_APPLICATION = (byte) 0x5A;
    static public final byte READ_DATA = (byte) 0xBD;
    static final byte READ_RECORD = (byte) 0xBB;
    static final byte GET_VALUE = (byte) 0x6C;
    static final byte GET_FILES = (byte) 0x6F;
    static final byte GET_FILE_SETTINGS = (byte) 0xF5;

    // Status codes
    static final byte OPERATION_OK = (byte) 0x00;
    static final byte PERMISSION_DENIED = (byte) 0x9D;
    static final byte AUTHENTICATION_ERROR = (byte) 0xAE;
    static public final byte ADDITIONAL_FRAME = (byte) 0xAF;

    private IsoDep mTagTech;

    public DesfireProtocol(IsoDep tagTech) {
        mTagTech = tagTech;
    }

    public DesfireManufacturingData getManufacturingData() throws Exception {
        byte[] respBuffer = sendRequest(GET_MANUFACTURING_DATA, true);

        if (respBuffer.length != 28)
            throw new IllegalArgumentException("Invalid response");

        return new DesfireManufacturingData(respBuffer);
    }

    /**
     * Gets an Application List from the card.
     *
     * Note that this method treats the card IDs as big-endian, though the DESFire protocol defines
     * them as little-endian. However, this means Integer.toHexString() on the numbers in Java makes
     * the bytes come out as the same order that is on the card / ISO14a frames.
     *
     * @return Array of integers representing DESFire application IDs, in big-endian.
     * @throws Exception on communication failures.
     */
    public int[] getAppList() throws Exception {
        byte[] appDirBuf = sendRequest(GET_APPLICATION_DIRECTORY, true);

        int[] appIds = new int[appDirBuf.length / 3];

        for (int app = 0; app < appDirBuf.length; app += 3) {
            appIds[app / 3] = Utils.byteArrayToInt(appDirBuf, app, 3);
        }

        return appIds;
    }

    /**
     * Selects an Application ID on the card.
     *
     * Note that this method treats the card IDs as big-endian, though the DESFire protocol defines
     * them as little-endian. However, this means Integer.toHexString() on the numbers in Java makes
     * the bytes come out as the same order that is on the card / ISO14a frames.
     *
     * Note that NXP TagInfo shows the application ID with endian reversed.
     * @param appId App ID, in big-endian.
     * @throws Exception on communication failures.
     */
    public void selectApp(int appId) throws Exception {
        byte[] appIdBuff = Utils.integerToByteArray(appId, 3);
        sendRequest(SELECT_APPLICATION, true, appIdBuff);
    }

    public int[] getFileList() throws Exception {
        byte[] buf = sendRequest(GET_FILES, true);
        int[] fileIds = new int[buf.length];
        for (int x = 0; x < buf.length; x++) {
            fileIds[x] = (int) buf[x];
        }
        return fileIds;
    }

    public DesfireFileSettings getFileSettings(int fileNo) throws Exception {
        byte[] data = sendRequest(GET_FILE_SETTINGS, true, (byte) fileNo);
        return DesfireFileSettings.create(data);
    }

    public byte[] readFile(int fileNo) throws Exception {
        return sendRequest(READ_DATA, true, (byte) fileNo,
                (byte) 0x0, (byte) 0x0, (byte) 0x0,
                (byte) 0x0, (byte) 0x0, (byte) 0x0);
    }

    public byte[] readRecord(int fileNum) throws Exception {
        return sendRequest(READ_RECORD, true, (byte) fileNum,
                (byte) 0x0, (byte) 0x0, (byte) 0x0,
                (byte) 0x0, (byte) 0x0, (byte) 0x0);
    }

    public byte[] getValue(int fileNum) throws Exception {
        return sendRequest(GET_VALUE, true, (byte) fileNum);
    }

    public byte[] sendUnlock(int keyNum) throws Exception {
        return sendRequest(UNLOCK, false, (byte) keyNum);
    }

    /**
     * Sends a DESFire command to the card, but wrapped in a ISO7816 APDU.
     *
     * @param command Command to send.
     * @param getAdditionalFrame If true, this will automatically request additional frames from
     *                           the card, if the card returns {@link #ADDITIONAL_FRAME}, and append
     *                           these frames to the final output.
     * @param parameters (optional) parameters to the DESFire command.
     * @return The response(s) from the card.
     * @throws IllegalArgumentException If the response was invalid.
     * @throws IllegalStateException If the status code was unknown / unhandled.
     * @throws AccessControlException If permission was denied or there was some authentication
     *                                problem.
     * @throws IOException If communication with the card failed.
     */
    @NonNull
    private byte[] sendRequest(byte command, boolean getAdditionalFrame, byte... parameters) throws IllegalArgumentException, IllegalStateException, AccessControlException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final boolean enableTracing = Utils.enableTracing();

        if (enableTracing) {
            Log.d(TAG, String.format(Locale.ENGLISH, ">>> cmd=%x, params=%s", command, Utils.getHexString(parameters)));
        }

        byte[] sendBuffer = wrapMessage(command, parameters);

        if (enableTracing) {
            Log.d(TAG, ">>> " + Utils.getHexString(sendBuffer));
        }

        byte[] recvBuffer = mTagTech.transceive(sendBuffer);
        if (enableTracing) {
            Log.d(TAG, "<<< " + Utils.getHexString(recvBuffer));
        }

        while (true) {
            if (recvBuffer[recvBuffer.length - 2] != (byte) 0x91) {
                throw new IllegalArgumentException("Invalid response: " + Utils.getHexString(recvBuffer));
            }

            output.write(recvBuffer, 0, recvBuffer.length - 2);

            byte status = recvBuffer[recvBuffer.length - 1];
            if (status == OPERATION_OK) {
                break;
            } else if (status == ADDITIONAL_FRAME) {
                if (!getAdditionalFrame)
                    break;

                if (enableTracing) {
                    Log.d(TAG, ">>> (requesting additional frame)");
                }

                recvBuffer = mTagTech.transceive(wrapMessage(GET_ADDITIONAL_FRAME));

                if (enableTracing) {
                    Log.d(TAG, "<<< " + Utils.getHexString(recvBuffer));
                }
            } else if (status == PERMISSION_DENIED) {
                throw new AccessControlException("Permission denied");
            } else if (status == AUTHENTICATION_ERROR) {
                throw new AccessControlException("Authentication error");
            } else {
                throw new IllegalStateException("Unknown status code: " + Integer.toHexString(status & 0xFF));
            }
        }

        return output.toByteArray();
    }


    /**
     * Wraps a DESFire command in a ISO 7816-style APDU.
     * @param command DESFire command to send
     * @param parameters Additional parameters to a command.
     * @return A wrapped command.
     */
    private byte[] wrapMessage(byte command, byte... parameters) {
        byte[] output = new byte[5 + (parameters.length == 0 ? 0 : 1 + parameters.length)];
        output[0] = (byte) 0x90;
        output[1] = command;
        output[2] = 0;
        output[3] = 0;

        if (parameters.length > 0) {
            output[4] = (byte)parameters.length;
            System.arraycopy(parameters, 0, output, 5, parameters.length);
        }

        output[output.length - 1] = 0;
        return output;
    }

    public byte[] sendAdditionalFrame(byte[] bytes) throws IOException {
        return sendRequest(ADDITIONAL_FRAME, false, bytes);
    }
}
