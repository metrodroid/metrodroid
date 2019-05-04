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

package au.id.micolous.metrodroid.card.desfire

import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.ImmutableByteArray.Companion.plus
import au.id.micolous.metrodroid.util.toImmutable

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
class DesfireProtocol(private val mTagTech: CardTransceiver) {

    suspend fun getManufacturingData() = sendRequest(GET_MANUFACTURING_DATA, true)

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
    suspend fun getAppList(): IntArray {
        val appDirBuf = sendRequest(GET_APPLICATION_DIRECTORY, true)

        return IntArray(appDirBuf.size / 3) {
            appDirBuf.byteArrayToInt(it * 3, 3)
        }
    }

    suspend fun getFileList(): IntArray {
        val buf = sendRequest(GET_FILES, true)
        return IntArray(buf.size) { buf[it].toInt() }
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
    suspend fun selectApp(appId: Int) {
        sendRequest(SELECT_APPLICATION, true,
                (appId shr 16).toByte(), (appId shr 8).toByte(), appId.toByte())
    }

    suspend fun getFileSettings(fileNo: Int) =
            sendRequest(GET_FILE_SETTINGS, true, fileNo.toByte())

    suspend fun readFile(fileNo: Int) =
            sendRequest(READ_DATA, true, fileNo.toByte(),
                0x0.toByte(), 0x0.toByte(), 0x0.toByte(),
                0x0.toByte(), 0x0.toByte(), 0x0.toByte())

    suspend fun readRecord(fileNum: Int) = sendRequest(READ_RECORD, true, fileNum.toByte(),
                0x0.toByte(), 0x0.toByte(), 0x0.toByte(),
                0x0.toByte(), 0x0.toByte(), 0x0.toByte())

    suspend fun getValue(fileNum: Int) = sendRequest(GET_VALUE, true, fileNum.toByte())

    suspend fun sendUnlock(keyNum: Int) = sendRequest(UNLOCK, false, keyNum.toByte())

    private suspend fun sendRequest(command: Byte,
                                    getAdditionalFrame: Boolean,
                                    vararg parameters: Byte) =
            sendRequest(command, getAdditionalFrame, parameters.toImmutable())

    private suspend fun sendRequest(command: Byte,
                                    getAdditionalFrame: Boolean,
                                    parameters: ImmutableByteArray): ImmutableByteArray {
        var output = ImmutableByteArray.empty()

        val sendBuffer = wrapMessage(command, parameters)
        //Log.d(TAG, "Send: " + Utils.getHexString(sendBuffer));
        var recvBuffer = mTagTech.transceive(sendBuffer)
        //Log.d(TAG, "Recv: " + Utils.getHexString(recvBuffer));

        loop@ while (true) {
            if (recvBuffer[recvBuffer.size - 2] != 0x91.toByte()) {
                throw IllegalArgumentException("Invalid response: $recvBuffer")
            }

            output = output.addSlice(recvBuffer, 0, recvBuffer.size - 2)

            val status = recvBuffer[recvBuffer.size - 1]
            when (status) {
                OPERATION_OK -> break@loop
                ADDITIONAL_FRAME -> {
                    if (!getAdditionalFrame)
                        break@loop
                    recvBuffer = mTagTech.transceive(wrapMessage(GET_ADDITIONAL_FRAME,
                            ImmutableByteArray.empty()))
                    //Log.d(TAG, "Recv: (additional) " + Utils.getHexString(recvBuffer));
                }
                PERMISSION_DENIED -> throw UnauthorizedException("Permission denied")
                AUTHENTICATION_ERROR -> throw UnauthorizedException("Authentication error")
                else -> throw IllegalStateException("Unknown status code: $status")
            }
        }

        return output
    }


    /**
     * Wraps a DESFire command in a ISO 7816-style APDU.
     * @param command DESFire command to send
     * @param parameters Additional parameters to a command.
     * @return A wrapped command.
     */
    private fun wrapMessage(command: Byte, parameters: ImmutableByteArray): ImmutableByteArray {
        // 1
        var output = ImmutableByteArray.ofB(0x90, command, 0, 0)

        if (parameters.isNotEmpty()) {
            output += parameters.size.toByte() + parameters
        }

        return output + 0.toByte()
    }

    suspend fun sendAdditionalFrame(bytes: ImmutableByteArray) =
            sendRequest(ADDITIONAL_FRAME, false, bytes)

    companion object {
        internal const val TAG = "DesfireProtocol"

        // Commands
        const val UNLOCK = 0x0A.toByte()
        const val GET_MANUFACTURING_DATA = 0x60.toByte()
        internal const val GET_APPLICATION_DIRECTORY = 0x6A.toByte()
        internal const val GET_ADDITIONAL_FRAME = 0xAF.toByte()
        internal const val SELECT_APPLICATION = 0x5A.toByte()
        const val READ_DATA = 0xBD.toByte()
        internal const val READ_RECORD = 0xBB.toByte()
        internal const val GET_VALUE = 0x6C.toByte()
        internal const val GET_FILES = 0x6F.toByte()
        internal const val GET_FILE_SETTINGS = 0xF5.toByte()

        // Status codes
        internal const val OPERATION_OK = 0x00.toByte()
        internal const val PERMISSION_DENIED = 0x9D.toByte()
        internal const val AUTHENTICATION_ERROR = 0xAE.toByte()
        const val ADDITIONAL_FRAME = 0xAF.toByte()
    }
}
