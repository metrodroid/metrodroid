/*
 * UltralightProtocol.java
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
package au.id.micolous.metrodroid.card.ultralight;

import android.nfc.tech.MifareUltralight;
import android.util.Log;

import au.id.micolous.metrodroid.util.Utils;

import java.io.IOException;
import java.util.Locale;

/**
 * Low level commands for MIFARE Ultralight.
 *
 * Android has MIFARE Ultralight support, but it is quite limited. It doesn't support detection of
 * EV1 cards, and also doesn't reliably detect Ultralight C cards. This class uses some
 * functionality adapted from the Proxmark3, as well as sniffed communication from NXP TagInfo.
 *
 * Reference:
 * MF0ICU1 (Ultralight): https://www.nxp.com/docs/en/data-sheet/MF0ICU1.pdf
 * MF0ICU2 (Ultralight C): https://www.nxp.com/docs/en/data-sheet/MF0ICU2_SDS.pdf
 * MF0UCx1 (Ultralight EV1): https://www.nxp.com/docs/en/data-sheet/MF0ULX1.pdf
 * MIFARE Commands: https://www.nxp.com/docs/en/application-note/AN10833.pdf
 */

class UltralightProtocol {
    static final String TAG = "UltralightProtocol";

    enum UltralightType {
        /** Unknown type */
        UNKNOWN(-1),
        /** MIFARE Ultralight (MF0ICU1), 15 pages */
        MF0ICU1(15),
        /** MIFARE Ultralight C (MF0ICU2), 47 pages (but pages 44-47 are masked), 3DES */
        MF0ICU2(43),
        /** MIFARE Ultralight EV1 (MF0UL11), 19 pages */
        EV1_MF0UL11(19),
        /** MIFARE Ultralight EV1 (MF0UL21), 40 pages */
        EV1_MF0UL21(40);

        /** Number of pages of memory that the card supports. */
        int pageCount;

        UltralightType(int pageCount) {
            this.pageCount = pageCount;
        }
    }

    // Commands
    static final byte GET_VERSION = (byte) 0x60;
    static final byte AUTH_1 = (byte) 0x1a;
    static final byte HALT = (byte) 0x50;

    // Status codes
    static final byte AUTH_ANSWER = (byte) 0xAF;

    private MifareUltralight mTagTech;

    UltralightProtocol(MifareUltralight tagTech) {
        mTagTech = tagTech;
    }

    /**
     * Gets the MIFARE Ultralight card type.
     *
     * Android has MIFAREUltralight.getType(), but this is lacking:
     *
     * 1. It cannot detect Ultralight EV1 cards correctly, which have different memory sizes.
     *
     * 2. It cannot detect the size of fully locked cards correctly.
     *
     * This is a much more versatile test, based on sniffing what NXP TagInfo does, and Proxmark3's
     * GetHF14AMfU_Type function. Android can't do bad checksums (eg: PM3 Fudan/clone check) and
     * Metrodroid never writes to cards (eg: PM3 Magic check), so we don't do all of the checks.
     *
     * @return MIFARE Ultralight card type.
     * @throws IOException On card communication error (eg: reconnects)
     */
    UltralightType getCardType() throws IOException {
        // Try EV1's GET_VERSION command
        // This isn't supported by non-UL EV1s, and will cause those cards to disconnect.
        byte[] b;
        try {
            b = getVersion();
        } catch (IOException e) {
            Log.d(TAG, "getVersion returned error, not EV1", e);
            b = null;
        }

        if (b != null) {
            if (b.length != 8) {
                Log.d(TAG, String.format(Locale.ENGLISH, "getVersion didn't return 8 bytes, got (%d instead): %s", b.length, Utils.getHexString(b)));
                return UltralightType.UNKNOWN;
            }

            if (b[2] != 0x03) {
                // TODO: PM3 notes that there are a number of NTAG which respond to this command, and look similar to EV1.
                Log.d(TAG, String.format(Locale.ENGLISH, "getVersion got a tag response with non-EV1 product code (%d): %s", b[2], Utils.getHexString(b)));
                return UltralightType.UNKNOWN;
            }

            // EV1 version detection.

            // Datasheet suggests we should do some maths here to allow for future card types,
            // however for the EV1_MF0UL11 we get an inexact data length. PM3 does the check this
            // way as well, and locked page reads all look the same.
            switch (b[6]) {
                case 0x0b:
                    return UltralightType.EV1_MF0UL11;
                case 0x0e:
                    return UltralightType.EV1_MF0UL21;
                default:
                    Log.d(TAG, String.format(Locale.ENGLISH,"getVersion returned unknown storage size (%d): %s", b[6], Utils.getHexString(b)));
                    return UltralightType.UNKNOWN;
            }
        } else {
            // Reconnect the tag
            mTagTech.close();
            mTagTech.connect();
        }

        // Try to get a nonce for 3DES authentication with Ultralight C.
        try {
            b = auth1();

            if (b == null) throw new IOException("auth1 returned null");
        } catch (IOException e) {
            // Non-C cards will disconnect here.
            Log.d(TAG, "auth1 returned error, not Ultralight C.", e);

            // TODO: PM3 says NTAG 203 (with different memory size) also looks like this.

            // Reconnect the tag
            mTagTech.close();
            mTagTech.connect();

            return UltralightType.MF0ICU1;
        }

        Log.d(TAG, String.format(Locale.ENGLISH, "auth1 said = %s", Utils.getHexString(b)));

        // To continue, we need to halt the auth attempt.
        halt();

        // Reconnect
        mTagTech.close();
        mTagTech.connect();

        return UltralightType.MF0ICU2;
    }

    /**
     * Gets the version data from the card. This only works with MIFARE Ultralight EV1 cards.
     * @return byte[] containing data according to Table 15 in MFU-EV1 datasheet.
     * @throws IOException on card communication failure, or if the card does not support the
     *                     command.
     */
    byte[] getVersion() throws IOException {
        return sendRequest(GET_VERSION);
    }

    /**
     * Gets a nonce for 3DES authentication from the card. This only works on MIFARE Ultralight C
     * cards. Authentication is not implemented in Metrodroid or Android.
     * @return AUTH_ANSWER message from card.
     * @throws IOException on card communication failure, or if the card does not support the
     *                     command.
     */
    byte[] auth1() throws IOException {
        return sendRequest(AUTH_1, (byte) 0x00);
    }

    /**
     * Instructs the card to terminate its session. This is supported by all Ultralight cards.
     *
     * This will silently swallow all communication failures, as Android returning an error is
     * to be expected.
     */
    void halt() {
        try {
            sendRequest(HALT, (byte) 0x00);
        } catch (IOException e) {
            // When the card halts, the tag may report an error up through the stack. This is fine.
            // Unfortunately we can't tell if the card was removed or we need to reset it.
            Log.d(TAG, "Discarding exception in halt, this probably expected...", e);
        }
    }


    private byte[] sendRequest(byte... data) throws IOException {
        Log.d(TAG, String.format(Locale.ENGLISH, "sent card: %s", data == null ? "null" : Utils.getHexString(data)));

        //TransceiveResult result = nonThrowingTransceive(data, true);
        return mTagTech.transceive(data);

    }

    // This is a bunch of code to poke at BasicTagTechnology, and get some better error codes.
    //
    // This isn't used _now_, but may be useful in the future in order to get at different error
    // states from the NFC driver.

    /*
    public class TransceiveResult {
        int result;
        byte[] responseData;
    }

    private TransceiveResult nonThrowingTransceive(byte[] data, boolean raw)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        // Adapted from:
        // https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/nfc/tech/BasicTagTechnology.java#142

        // Android calls TransceiveResult.getResponseOrThrow() whenever we try to talk to the card
        // Instead, we want to be able to call this transceive operation without being blocked, and
        // get at the raw data we were passed back.

        Tag t = mTagTech.getTag();
        Method getTagService = t.getClass().getDeclaredMethod("getTagService");
        getTagService.setAccessible(true);
        Object tagService = getTagService.invoke(t);
        Method getServiceHandle = t.getClass().getDeclaredMethod("getServiceHandle");
        getServiceHandle.setAccessible(true);
        int serviceHandle = (Integer) getServiceHandle.invoke(t);

        Method transceive = tagService.getClass().getDeclaredMethod("transceive", int.class, byte[].class, boolean.class);
        transceive.setAccessible(true);
        Object transceiveResult = transceive.invoke(tagService, serviceHandle, data, raw);

        if (transceiveResult == null) {
            return null;
        } else {
            // Patch the values into our own thing
            TransceiveResult r = new TransceiveResult();
            Field mResult = transceiveResult.getClass().getDeclaredField("mResult");
            mResult.setAccessible(true);
            r.result = mResult.getInt(transceiveResult);

            Field mResponseData = transceiveResult.getClass().getDeclaredField("mResponseData");
            mResponseData.setAccessible(true);
            r.responseData = (byte[]) mResponseData.get(transceiveResult);

            if (r.responseData == null) {
                r.responseData = new byte[0];
            }

            return r;
        }
    }
    */

}
