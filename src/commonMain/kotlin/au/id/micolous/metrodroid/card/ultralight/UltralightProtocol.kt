/*
 * UltralightProtocol.kt
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
import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import kotlinx.serialization.Serializable

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

internal class UltralightProtocol(private val mTagTech: CardTransceiver) {

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
     * @throws CardTransceiveException On card communication error (eg: reconnects)
     */
    fun getCardType(): UltralightCard.UltralightTypeRaw {
        // Try EV1's GET_VERSION command
        // This isn't supported by non-UL EV1s, and will cause those cards to disconnect.
        try {
            return UltralightCard.UltralightTypeRaw(versionCmd = getVersion())
        } catch (e: CardTransceiveException) {
            Log.d(TAG, "getVersion returned error, not EV1", e)
        } catch (e: CardLostException) {
            Log.d(TAG, "getVersion returned error, not EV1", e)
        }

        // Reconnect the tag
        mTagTech.reconnect()

        // Try to get a nonce for 3DES authentication with Ultralight C.
        try {
            val b2 = auth1()
            Log.d(TAG, "auth1 said = $b2")
        } catch (e: CardTransceiveException) {
            // Non-C cards will disconnect here.
            Log.d(TAG, "auth1 returned error, not Ultralight C.", e)

            // TODO: PM3 says NTAG 203 (with different memory size) also looks like this.

            mTagTech.reconnect()
            return UltralightCard.UltralightTypeRaw(repliesToAuth1 = false)
        } catch (e: CardLostException) {
            // Non-C cards will disconnect here.
            Log.d(TAG, "auth1 returned error, not Ultralight C.", e)

            // TODO: PM3 says NTAG 203 (with different memory size) also looks like this.

            mTagTech.reconnect()
            return UltralightCard.UltralightTypeRaw(repliesToAuth1 = false)
        }

        // To continue, we need to halt the auth attempt.
        halt()
        mTagTech.reconnect()

        return UltralightCard.UltralightTypeRaw(repliesToAuth1 = true)
    }

    /**
     * Gets the version data from the card. This only works with MIFARE Ultralight EV1 cards.
     * @return byte[] containing data according to Table 15 in MFU-EV1 datasheet.
     * @throws CardTransceiveException on card communication failure, or if the card does not support the
     * command.
     */
    private fun getVersion(): ImmutableByteArray = sendRequest(GET_VERSION)

    /**
     * Gets a nonce for 3DES authentication from the card. This only works on MIFARE Ultralight C
     * cards. Authentication is not implemented in Metrodroid or Android.
     * @return AUTH_ANSWER message from card.
     * @throws CardTransceiveException on card communication failure, or if the card does not support the
     * command.
     */
    private fun auth1(): ImmutableByteArray = sendRequest(AUTH_1, 0x00.toByte())

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
        } catch (e: CardLostException) {
            Log.d(TAG, "Discarding disconnect in halt, this probably expected...", e)
        }

    }

    private fun sendRequest(vararg data: Byte): ImmutableByteArray {
        Log.d(TAG,  "sent card: " + ImmutableByteArray.getHexString(data))

        return mTagTech.transceive(data.toImmutable())
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
