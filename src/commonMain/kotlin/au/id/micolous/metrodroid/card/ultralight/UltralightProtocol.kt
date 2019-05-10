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
package au.id.micolous.metrodroid.card.ultralight

import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.CardTransceiveException
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray

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
 * NTAG213/215/216: https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf
 * MIFARE Commands: https://www.nxp.com/docs/en/application-note/AN10833.pdf
 */

internal class UltralightProtocol(private val mTagTech: UltralightTransceiver) {

    /**
     * Gets the MIFARE Ultralight card type.
     *
     * Android has `MIFAREUltralight.getType()`, but this is lacking:
     *
     * 1. It cannot detect Ultralight EV1 cards correctly, which have different memory sizes.
     *
     * 2. It cannot detect the size of fully locked cards correctly.
     *
     * This is a much more versatile test, based on sniffing what NXP TagInfo does, and Proxmark3's
     * `GetHF14AMfU_Type` function. Android can't do bad checksums (eg: PM3 Fudan/clone check) and
     * Metrodroid never writes to cards (eg: PM3 Magic check), so we don't do all of the checks.
     *
     * @return MIFARE Ultralight card type.
     * @throws IOException On card communication error (eg: reconnects)
     */
    fun getCardType(): UltralightType {
        // Try EV1's GET_VERSION command
        // This isn't supported by non-UL EV1s, and will cause those cards to disconnect.
        val b = try {
            getVersion()
        } catch (e: CardTransceiveException) {
            Log.d(TAG, "getVersion returned error, not EV1", e)
            null
        } catch (e: CardLostException) {
            Log.d(TAG, "getVersion returned error, not EV1", e)
            null
        }

        if (b != null) {
            if (b.size != 8) {
                Log.d(TAG, "getVersion didn't return 8 bytes, got (${b.size} instead): " + ImmutableByteArray.getHexString(b))
                return UltralightType.UNKNOWN
            }

            if (b[2].toInt() == 0x04) {
                // Datasheet suggests we should do some maths here to allow for future card types,
                // however for all cards, we get an inexact data length. A locked page read does a
                // NAK, but an authorised read will wrap around to page 0x00.
                return when (b[6].toInt()) {
                    0x0F -> UltralightType.NTAG213
                    0x11 -> UltralightType.NTAG215
                    0x13 -> UltralightType.NTAG216
                    else -> {
                        Log.d(TAG, "getVersion returned unknown storage size (${b[6]}): %s" + ImmutableByteArray.getHexString(b))
                        UltralightType.UNKNOWN
                    }
                }
            }

            if (b[2].toInt() != 0x03) {
                // TODO: PM3 notes that there are a number of NTAG which respond to this command, and look similar to EV1.
                Log.d(TAG, "getVersion got a tag response with non-EV1 product code (${b[2]}): " + ImmutableByteArray.getHexString(b))
                return UltralightType.UNKNOWN
            }

            // EV1 version detection.
            //
            // Datasheet suggests we should do some maths here to allow for future card types,
            // however for the EV1_MF0UL11 we get an inexact data length. PM3 does the check this
            // way as well, and locked page reads all look the same.
            return when (b[6].toInt()) {
                0x0b -> UltralightType.EV1_MF0UL11
                0x0e -> UltralightType.EV1_MF0UL21
                else -> {
                    Log.d(TAG, "getVersion returned unknown storage size (${b[6]}): " + ImmutableByteArray.getHexString(b))
                    UltralightType.UNKNOWN
                }
            }
        } else {
            // Reconnect the tag
            mTagTech.reconnect()
        }

        // Try to get a nonce for 3DES authentication with Ultralight C.
        try {
            val b2 = auth1() ?: throw CardTransceiveException("auth1 returned null")
            Log.d(TAG, "auth1 said = $b2")
        } catch (e: CardTransceiveException) {
            // Non-C cards will disconnect here.
            Log.d(TAG, "auth1 returned error, not Ultralight C.", e)

            // TODO: PM3 says NTAG 203 (with different memory size) also looks like this.

            mTagTech.reconnect()
            return UltralightType.MF0ICU1
        } catch (e: CardLostException) {
            // Non-C cards will disconnect here.
            Log.d(TAG, "auth1 returned error, not Ultralight C.", e)

            // TODO: PM3 says NTAG 203 (with different memory size) also looks like this.

            mTagTech.reconnect()
            return UltralightType.MF0ICU1
        }

        // To continue, we need to halt the auth attempt.
        halt()
        mTagTech.reconnect()

        return UltralightType.MF0ICU2
    }

    /**
     * Gets the version data from the card. This only works with MIFARE Ultralight EV1 cards.
     * @return byte[] containing data according to Table 15 in MFU-EV1 datasheet.
     * @throws IOException on card communication failure, or if the card does not support the
     * command.
     */
    private fun getVersion(): ByteArray = sendRequest(GET_VERSION)

    internal enum class UltralightType(
            /** Number of pages of memory that the card supports.  */
            val pageCount: Int) {
        /** Unknown type  */
        UNKNOWN(-1),
        /** MIFARE Ultralight (MF0ICU1), 16 pages  */
        MF0ICU1(16),
        /** MIFARE Ultralight C (MF0ICU2), 48 pages (but pages 44-47 are masked), 3DES  */
        MF0ICU2(44),
        /** MIFARE Ultralight EV1 (MF0UL11), 20 pages  */
        EV1_MF0UL11(20),
        /** MIFARE Ultralight EV1 (MF0UL21), 41 pages  */
        EV1_MF0UL21(41),

        NTAG213(45),
        NTAG215(135),
        NTAG216(231)
    }

    /**
     * Gets a nonce for 3DES authentication from the card. This only works on MIFARE Ultralight C
     * cards. Authentication is not implemented in Metrodroid or Android.
     * @return AUTH_ANSWER message from card.
     * @throws IOException on card communication failure, or if the card does not support the
     * command.
     */
    private fun auth1(): ByteArray {
        return sendRequest(AUTH_1, 0x00.toByte())
    }

    /**
     * Instructs the card to terminate its session. This is supported by all Ultralight cards.
     *
     * This will silently swallow all communication failures, as Android returning an error is
     * to be expected.
     */
    private fun halt() {
        try {
            sendRequest(HALT, 0x00.toByte())
        } catch (e: CardTransceiveException) {
            // When the card halts, the tag may report an error up through the stack. This is fine.
            // Unfortunately we can't tell if the card was removed or we need to reset it.
            Log.d(TAG, "Discarding exception in halt, this probably expected...", e)
        }

    }

    private fun sendRequest(vararg data: Byte): ByteArray {
        Log.d(TAG,  "sent card: " + ImmutableByteArray.getHexString(data))

        return mTagTech.transceive(data)
    }

    companion object {
        private const val TAG = "UltralightProtocol"

        // Commands
        private const val GET_VERSION = 0x60.toByte()
        private const val AUTH_1 = 0x1a.toByte()
        private const val HALT = 0x50.toByte()

        // Status codes
        const val AUTH_ANSWER = 0xAF.toByte()
    }
}
