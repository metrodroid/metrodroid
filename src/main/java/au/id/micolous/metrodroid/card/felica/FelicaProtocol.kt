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
package au.id.micolous.metrodroid.card.felica

import android.nfc.TagLostException
import android.nfc.tech.NfcF
import android.util.Log

import org.apache.commons.lang3.ArrayUtils

import java.io.IOException
import java.util.ArrayList

import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Utils

/**
 * Protocol implementation for FeliCa and FeliCa Lite.
 *
 * This is adapted from nfc-felica-lib, and is still a work in progress to refactor it to be more
 * like ISO7816Protocol, and translate all the documentation into English.
 *
 * Note: This doesn't implement any of the write commands.
 *
 * FeliCa Card User's Manual, Excerpted Edition:
 * https://www.sony.net/Products/felica/business/tech-support/data/card_usersmanual_2.02.pdf
 * nfcpy
 * https://github.com/nfcpy/nfcpy/blob/master/src/nfc/tag/tt3.py
 * https://github.com/nfcpy/nfcpy/blob/master/src/nfc/tag/tt3_sony.py
 */

class FelicaProtocol(/** Connection to the FeliCa card  */
                     private val tagTech: NfcF) {

    /** IDm (Manufacturing ID) for the card  */
    /**
     * Gets the IDm (Manufacture Identifier).  This is the serial number of a FeliCa card.
     */
    var idm: ImmutableByteArray? = null
        private set

    /** PMm (Manufacturing Parameters) for the card  */
    /**
     * Gets the PMm (Manufacturing Parameters).
     */
    var pmm: ImmutableByteArray? = null
        private set

    /**
     * Gets a list of system codes supported by the card.
     *
     * @throws TagLostException if the tag went out of the field
     */
    //request systemCode
    // No system codes were received from the card.
    val systemCodeList: IntArray
        @Throws(IOException::class, TagLostException::class)
        get() {
            val retBytes = sendRequest(COMMAND_REQUEST_SYSTEMCODE, idm) ?: return IntArray(0)

            var count = retBytes.byteArrayToInt(9, 1)

            if (10 + count * 2 > retBytes.size) {
                Log.w(TAG, "Got too few bytes from FeliCa for system code list, truncating...")
                count = (retBytes.size - 10) / 2
            }

            val ret = IntArray(count)
            for (i in 0 until count) {
                ret[i] = retBytes.byteArrayToInt(10 + i * 2, 2)
            }

            return ret
        }

    /**
     * Gets a list of service codes supported by the card.
     *
     * This is done by repeatedly calling SEARCH_SERVICECODE until no more values are returned.
     *
     * The service codes in "corrected" byte order -- SEARCH_SERVICECODE returns service codes in
     * little endian, and the read/write commands take in service codes in little endian.
     *
     * One must select a system code first with polling().
     */
    @Throws(IOException::class, TagLostException::class)
    fun getServiceCodeList(): IntArray {
        val serviceCodeList = mutableListOf<Int>()

        // index 0 = root area
        for (index in 1..0xffff) {
            val bytes = searchServiceCode(index)

            if (bytes.size != 2 && bytes.size != 4) {
                // Unexpected data length, stop now!
                break
            }

            // 2 bytes indicates a service code.
            //
            // Note: we handle the service code internally as if it were little endian, as
            // "read without encryption" takes the service code parameter in little endian
            if (bytes.size == 2) {
                val code = bytes.byteArrayToIntReversed()

                if (code == 0xffff) {
                    // No more data available.
                    break
                }

                serviceCodeList.add(code)
            }

            // 4 byte responses are area codes. For Metrodroid, we ignore this part of the
            // structure and haven't ever used it -- so we just keep on processing...
        }
        return serviceCodeList.toIntArray()
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
    @Throws(TagLostException::class, IOException::class)
    private fun sendRequest(
            commandCode: Byte, idm: ImmutableByteArray?, vararg data: Byte): ImmutableByteArray? {
        if (commandCode.toInt() and 0x01 != 0) {
            throw IllegalArgumentException("commandCode must be even")
        }

        val length = 2 + data.size + (idm?.size ?: 0)

        val sendBuffer = ByteArray(length)
        sendBuffer[0] = length.toByte()
        sendBuffer[1] = commandCode

        idm?.copyInto(sendBuffer, 2)
        data.copyInto(sendBuffer, (idm?.size ?: 0) + 2)

        if (ENABLE_TRACING) {
            Log.d(TAG, ">>> " + Utils.getHexString(sendBuffer))
        }

        val recvBuffer = tagTech.transceive(sendBuffer)
        if (recvBuffer == null) {
            if (ENABLE_TRACING) {
                Log.d(TAG, "<<< (null)")
            }

            return null
        }

        if (ENABLE_TRACING) {
            Log.d(TAG, "<<< " + Utils.getHexString(recvBuffer))
        }

        // Check command code
        if (commandCode + 1 != recvBuffer[1].toInt()) {
            throw IOException("response had unexpected command code")
        }

        // Automatically strip off the length prefix before returning it.
        return ImmutableByteArray.fromByteArray(recvBuffer).sliceOffLen(1)
    }

    /**
     * Polls for a card with the given system code.
     *
     * Automatically sets the IDm and PMm of the card based on the response.
     *
     * This does not pass the IDm as a parameter, and may select *any* nearby tag.  If no
     * IDm or PMm is given in the response, this will *unset* these values.
     *
     * @param systemCode System code to search for, or SYSTEMCODE_ANY to scan for any card.
     * @return The response from the card, including the IDm and PMm
     * @throws IOException On communication errors
     * @throws TagLostException If the tag moves out of the field, or there is no response
     */
    @Throws(IOException::class, TagLostException::class)
    fun polling(systemCode: Int): ImmutableByteArray? {
        val res = sendRequest(COMMAND_POLLING, null,
                (systemCode shr 8).toByte(), // System code (upper byte)
                (systemCode and 0xff).toByte(), // System code (lower byte)
                0x01.toByte(), // Request code (system code request)
                0x07.toByte()) // Maximum number of time slots to respond
                ?: return null

        if (res.size >= 9) {
            idm = res.sliceOffLen(1, 8)
        } else {
            idm = null
        }

        if (res.size >= 17) {
            pmm = res.sliceOffLen(9, 8)
        } else {
            pmm = null
        }

        return res
    }

    /**
     * Polls for the given system code, and returns the IDm of the responding card.
     *
     * @param systemCode System code to search for, or SYSTEMCODE_ANY to scan for any card.
     * @return The responding card's IDm.
     * @throws IOException On communication errors
     * @throws TagLostException If the tag moves out of the field, or there is no response
     */
    @Throws(IOException::class, TagLostException::class)
    fun pollingAndGetIDm(systemCode: Int): ImmutableByteArray? {
        polling(systemCode)
        return idm
    }

    /**
     * Get the n'th service code on the card.
     *
     * This allows mapping of the physical service code number (1, 2, 3...) to the logical service
     * code number (0x48, 0x4a, 0x88...).
     *
     * Note: this command is not publicly documented. nfcpy has the best (public) notes on this.
     *
     * @param index The index of service or area number to get. This is converted to little-endian
     * before being transmitted to the card.
     * @return If 2 bytes, a service code number.
     * If 4 bytes, an area code followed by a maximum service number for the area.
     * Returns 2 bytes of 0xffff when the card reaches EOF.
     * All return values are little endian.
     */
    @Throws(IOException::class, TagLostException::class)
    private fun searchServiceCode(index: Int): ImmutableByteArray {
        if (index < 0 || index > 0xffff) {
            throw IllegalArgumentException("index must be in range 0-0xffff")
        }

        val res = sendRequest(COMMAND_SEARCH_SERVICECODE, idm,
                (index and 0xff).toByte(), // little endian
                (index shl 8).toByte())

        return if (res == null || res.isEmpty()) {
            ImmutableByteArray.empty()
        } else res.sliceOffLen(9)

    }

    /**
     * Reads a given service code without encryption.
     *
     * @param serviceCode The service code to read. This is converted to little endian before being
     * transmitted to the card.
     * @param blockNumber Block to read from the cord.
     * @throws TagLostException if the tag went out of the field
     */
    @Throws(IOException::class, TagLostException::class)
    fun readWithoutEncryption(
            serviceCode: Int, blockNumber: Byte): ImmutableByteArray? {


        // read without encryption
        val resp = sendRequest(COMMAND_READ_WO_ENCRYPTION, idm,
                0x01.toByte(), // Number of service codes
                (serviceCode and 0xff).toByte(), // Service code (lower byte)
                (serviceCode shr 8).toByte(), // Service code (upper byte)
                0x01.toByte(), // Number of blocks to read
                0x80.toByte(), // Block (upper byte, always 0x80)
                blockNumber) ?: return null                       // Block (lower byte)

        return if (resp[9].toInt() != 0) {
            // Status flag 1
            null
        } else resp.sliceOffLen(12)

    }

    companion object {
        // CARD COMMANDS
        // Polling (s4.4.2)
        private const val COMMAND_POLLING: Byte = 0x00
        private const val RESPONSE_POLLING: Byte = 0x01

        // Request Service (s4.4.3)
        private const val COMMAND_REQUEST_SERVICE: Byte = 0x02
        private const val RESPONSE_REQUEST_SERVICE: Byte = 0x03

        // Request Response (s4.4.4)
        private const val COMMAND_REQUEST_RESPONSE: Byte = 0x04
        private const val RESPONSE_REQUEST_RESPONSE: Byte = 0x05

        // Read without encryption (s4.4.5)
        private const val COMMAND_READ_WO_ENCRYPTION: Byte = 0x06
        private const val RESPONSE_READ_WO_ENCRYPTION: Byte = 0x07

        // Write without encryption (s4.4.6)
        private const val COMMAND_WRITE_WO_ENCRYPTION: Byte = 0x08
        private const val RESPONSE_WRITE_WO_ENCRYPTION: Byte = 0x09

        // Search service code (s4.4.7, not documented publicly)
        private const val COMMAND_SEARCH_SERVICECODE: Byte = 0x0a
        private const val RESPONSE_SEARCH_SERVICECODE: Byte = 0x0b

        // Request system code (s4.4.8)
        private const val COMMAND_REQUEST_SYSTEMCODE: Byte = 0x0c
        private const val RESPONSE_REQUEST_SYSTEMCODE: Byte = 0x0d

        // Authentication 1 (s4.4.9, not documented publicly)
        private const val COMMAND_AUTHENTICATION1: Byte = 0x10
        private const val RESPONSE_AUTHENTICATION1: Byte = 0x11

        // Authentication 2 (s4.4.10, not documented publicly)
        private const val COMMAND_AUTHENTICATION2: Byte = 0x12
        private const val RESPONSE_AUTHENTICATION2: Byte = 0x13

        // Authenticated Read (s4.4.11, not documented publicly)
        private const val COMMAND_READ: Byte = 0x14
        private const val RESPONSE_READ: Byte = 0x15

        // Authenticated Write (s4.4.12, not documented publicly)
        private const val COMMAND_WRITE: Byte = 0x16
        private const val RESPONSE_WRITE: Byte = 0x17

        // SYSTEM CODES
        // Wildcard, matches any system code.
        const val SYSTEMCODE_ANY = 0xffff
        // FeliCa Lite
        const val SYSTEMCODE_FELICA_LITE = 0x88b4
        // NDEF (NFC Data Exchange Format)
        const val SYSTEMCODE_NDEF = 0x4000
        // Common Area (FeliCa Networks, Inc), used by IC (Suica) and Edy
        const val SYSTEMCODE_COMMON = 0xfe00

        // SERVICE CODES
        // FeliCa Lite, read-only mode
        const val SERVICE_FELICA_LITE_READONLY = 0x0b00
        // FeliCa Lite, read-write mode
        const val SERVICE_FELICA_LITE_READWRITE = 0x0900

        /**
         * If true, this turns on debug logs that show FeliCa communication.
         */
        private const val ENABLE_TRACING = true
        private val TAG = FelicaProtocol::class.java.simpleName
    }
}
