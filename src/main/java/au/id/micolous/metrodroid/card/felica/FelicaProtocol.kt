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
import android.util.Log
import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.Protocol
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.toImmutable
import java.io.IOException

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

class FelicaProtocol(
        tag: CardTransceiver,
        idm: ImmutableByteArray) : Protocol(tag, CardTransceiver.Protocol.JIS_X_6319_4) {

    /**
     * IDm (Manufacture Identifier).
     *
     * This is the serial number of a FeliCa card.
     */
    var idm: ImmutableByteArray
        private set

    init {
        this.idm = idm
    }

    /**
     * Gets the PMm (Manufacturing Parameters).
     */
    val pmm: ImmutableByteArray? get() = tag.pmm

    /**
     * Gets the default system code for the card.
     *
     * This is the system code that was selected when we first saw the card.
     */
    val defaultSystemCode: Int
        get() = tag.defaultSystemCode ?: 0

    /**
     * Gets a list of system codes supported by the card.
     *
     * @throws TagLostException if the tag went out of the field
     */
    fun getSystemCodeList(): IntArray {
        val res = sendRequest(COMMAND_REQUEST_SYSTEMCODE, 0)

        if (res.size < 10) {
            Log.w(TAG, "Got too few bytes from FeliCa for system code list, need at least 10")
            return intArrayOf()
        }

        var count = res.byteArrayToInt(9, 1)

        if (10 + count * 2 > res.size) {
            Log.w(TAG, "Got too few bytes from FeliCa for system code list, truncating...")
            count = (res.size - 10) / 2
        }

        return (0 until count).map {
            res.byteArrayToInt((it * 2) + 10, 2)
        }.toIntArray()
    }

    /**
     * FeliCa Lite cards don't respond to [getSystemCodeList], but do respond to Polling.
     *
     * If the card is a FeliCa lite, one should not try to select any other services on the card.
     *
     * @returns True if the card is a FeliCa Lite.
     */
    fun pollFelicaLite(): Boolean {
        return try {
            pollForSystemCode(SYSTEMCODE_FELICA_LITE) != null
        } catch (e: TagLostException) {
            Log.d(TAG, "Swallowing TagLostExecption", e)
            false
        }
    }

    /**
     * Gets a list of system codes supported by the card, by using Polling.
     *
     * This is needed for FeliCa Lite, as well as some older Octopus cards (and probably
     * first-generation SZT cards).
     */
    fun pollForSystemCodes(systemCodes: IntArray): IntArray {
        val r = mutableMapOf<Int, Int>()

        for (systemCode in systemCodes) {
            try {
                val id = pollForSystemCode(systemCode)
                if (id != null) {
                    r[id] = systemCode
                }
            } catch (e: TagLostException) {
                Log.d(TAG, "Swallowing TagLostException for system code $systemCode", e)
            }
        }

        if (r.isEmpty()) {
            return intArrayOf()
        }

        // Now we need to turn this map into an array.
        return (0 .. 0xf).map {
            r[it] ?: 0
        }.toIntArray()

        // TODO remove null entries at the end
    }

    /**
     * Gets a list of service codes supported by the card.
     *
     * This is done by repeatedly calling SEARCH_SERVICECODE until no more values are returned.
     *
     * The service codes in "corrected" byte order -- SEARCH_SERVICECODE returns service codes in
     * little endian, and the read/write commands take in service codes in little endian.
     *
     * One must select a system code first with pollForSystemCode().
     */
    @Throws(IOException::class, TagLostException::class)
    fun getServiceCodeList(systemNumber: Int): IntArray {
        val serviceCodeList = mutableListOf<Int>()

        // index 0 = root area
        for (index in 1..0xffff) {
            val bytes = searchServiceCode(systemNumber, index)

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
     * @param systemNumber If null, then requests with no IDm.
     * If set (0 - 15), then requests the IDm with the given system number.
     * If -1, then requests with [idm] unaltered.
     * @param data Command parameters
     * @return Response from the card, or null.
     * @throws TagLostException If the tag moves out of the NFC field
     * @throws IOException On communications errors
     */
    @Throws(TagLostException::class, IOException::class)
    private fun sendRequest(
            commandCode: Byte, systemNumber: Int?, vararg data: Byte): ImmutableByteArray {
        return sendRequest(commandCode, systemNumber, data.toImmutable())
    }

    @Throws(TagLostException::class, IOException::class)
    private fun sendRequest(
            commandCode: Byte, systemNumber: Int?): ImmutableByteArray {
        return sendRequest(commandCode, systemNumber, ImmutableByteArray.empty())
    }

    @Throws(TagLostException::class, IOException::class)
    private fun sendRequest(
            commandCode: Byte, systemNumber: Int?, data: ImmutableByteArray): ImmutableByteArray {
        if (commandCode.toInt() and 0x01 != 0) {
            throw IllegalArgumentException("commandCode must be even")
        }
        val idm = systemNumber?.let { getIdmForSystemNumber(idm, it) }

        val length = 2 + data.size + (idm?.size ?: 0)

        val header = ImmutableByteArray.ofB(length, commandCode)
        val sendBuffer = header + (idm ?: ImmutableByteArray.empty()) + data

        if (ENABLE_TRACING) {
            Log.d(TAG, ">>> ${sendBuffer.toHexString()}")
        }

        val recvBuffer = tag.transceive(sendBuffer)

        if (ENABLE_TRACING) {
            Log.d(TAG, "<<< " + recvBuffer.toHexString())
        }

        // Check command code
        if (commandCode + 1 != recvBuffer[1].toInt()) {
            throw IOException("response had unexpected command code")
        }

        // Automatically strip off the length prefix before returning it.
        return recvBuffer.sliceArray(1 until recvBuffer.size)
    }

    @Throws(TagLostException::class, IOException::class)
    private fun sendRequest(
            commandCode: Byte, systemNumber: Int?, vararg data: Number) =
            sendRequest(commandCode, systemNumber, data.toImmutable())

    /**
     * Polls for a given system code with the correct IDm.
     *
     * The Polling command selects **any** nearby tag.  This automatically filters out the bad tags.
     *
     * Use [resetMode] to select a system number on a specific tag.  This should only be used for
     * tags that don't respond to [getSystemCodeList].
     *
     * @param systemCode System code to search for, or SYSTEMCODE_ANY to scan for any card.
     * @param maxTries Maximum tries to scan for the card
     * @return The system number for the given system code, or null if it doesn't respond to that
     *         system code.
     * @throws IOException On communication errors
     * @throws TagLostException If the tag moves out of the field, or there is no response
     */
    @JvmOverloads
    @Throws(IOException::class, TagLostException::class)
    fun pollForSystemCode(systemCode: Int, maxTries: Int = 3): Int? {
        if (maxTries < 1) {
            throw IllegalArgumentException("maxTries must be at least 1")
        }
        val hexSystemCode = NumberUtils.intToHex(systemCode)

        for (attempt in 1..maxTries) {
            val res = sendRequest(COMMAND_POLLING, null,
                    systemCode shr 8,      // System code (upper byte)
                    systemCode and 0xff,   // System code (lower byte)
                    0x01,                  // Request code (system code request)
                    0x07)                  // Maximum number of time slots to respond

            // Because pollForSystemCode has no IDm parameter, check that we got what we wanted...
            if (res.size < 9) {
                // invalid IDm in response
                continue
            }

            val newIdm = res.sliceArray(1 until 9)
            if (idmEquals(idm, newIdm)) {
                // We have a match!
                val i = FelicaUtils.getSystemNumber(newIdm)
                Log.d(TAG, "Card ${idm.toHexString()} has system code $hexSystemCode at $i")
                return i
            } else {
                Log.d(TAG, "Card ${newIdm.toHexString()} responded to system code $hexSystemCode," +
                        " not ${idm.toHexString()}...")
            }
        }

        throw TagLostException("Tag $idm did not respond to $hexSystemCode after $maxTries " +
                "attempt(s)")
    }

    /**
     * Issues the Reset Mode command, which allows switching a system by its number and IDm.
     *
     * If the card does not support [getSystemCodeList], you may need to use [pollForSystemCode]
     * instead.
     */
    fun resetMode(systemNumber: Int) {
        val res = sendRequest(COMMAND_RESET_MODE, systemNumber,
                0, 0) // Reserved parameter, always specify 0x0000

        if (res.size < 11) {
            // Unexpected response length
            throw IOException("Unexpected reset response length (${res.size} != 11)")
        }

        if (res[9] != 0.toByte()) {
            throw IOException("Reset response error: ${res.sliceArray(9 until 11).toHexString()}")
        }
    }

    /**
     * Get the n'th service code on the card.
     *
     * This allows mapping of the physical service code number (1, 2, 3...) to the logical service
     * code number (0x4801, 0x4a01, 0x8081...).
     *
     * Note: this command is not publicly documented. nfcpy has the best (public) notes on this.
     *
     * @param systemNumber The system number to use for communication with the card.
     * @param index The index of service or area number to get. This is converted to little-endian
     * before being transmitted to the card.
     * @return If 2 bytes, a service code number.
     * If 4 bytes, an area code followed by a maximum service number for the area.
     * Returns 2 bytes of 0xffff when the card reaches EOF.
     * All return values are little endian.
     */
    @Throws(IOException::class, TagLostException::class)
    private fun searchServiceCode(systemNumber: Int, index: Int): ImmutableByteArray {
        if (index < 0 || index > 0xffff) {
            throw IllegalArgumentException("index must be in range 0-0xffff")
        }

        val res = sendRequest(COMMAND_SEARCH_SERVICECODE, systemNumber,
                index and 0xff, // little endian
                index shl 8)

        return if (res.isEmpty()) {
            ImmutableByteArray.empty()
        } else res.sliceArray(9 until res.size)

    }

    /**
     * Reads a given service code without encryption.
     *
     * @param serviceCode The service code to read. This is converted to little endian before being
     * transmitted to the card.
     * @param blockNumber Block to read from the cord.
     * @returns Block data, or null on read error.
     * @throws TagLostException if the tag went out of the field
     */
    @Throws(IOException::class, TagLostException::class)
    fun readWithoutEncryption(
            systemNumber: Int, serviceCode: Int, blockNumber: Int): ImmutableByteArray? {
        val r = ImmutableByteArray.ofB(
                0x01, // Number of service codes
                serviceCode and 0xff, // Service code (lower byte)
                serviceCode shr 8, // Service code (upper byte)
                0x01 // Number of blocks to read
        ) + encodeBlockRequest(blockNumber)

        val resp = sendRequest(COMMAND_READ_WO_ENCRYPTION, systemNumber, r)

        return if (resp[9].toInt() != 0) {
            // Status flag 1
            null
        } else resp.sliceArray(12 until resp.size)
    }

    /**
     * Reads multiple blocks without encryption.
     *
     * FeliCa Lite supports up to 4 blocks at once.
     *
     * JIS X 6319-4 specifies that up to 8 blocks may be read at once.
     *
     * The practical protocol limit is 15 (16*15+10+1 = 251 bytes), which is enforced.
     *
     * However, the regular FeliCa specification does not indicate an actual limit.
     */
    @Throws(IOException::class, TagLostException::class)
    fun readWithoutEncryption(
            systemNumber: Int, serviceCode: Int, blockNumbers: IntArray):
            Map<Int, ImmutableByteArray>? {
        if (blockNumbers.size > 15) {
            throw IllegalArgumentException("Can only read 15 blocks at a time, you gave " +
                    "${blockNumbers.size}")
        }

        if (blockNumbers.isEmpty()) {
            // No entries -- nothing to send to the card!
            return emptyMap()
        }

        val blockRequests = blockNumbers.map {
            encodeBlockRequest(it).toList()
        }.flatten().toByteArray()

        val resp = sendRequest(COMMAND_READ_WO_ENCRYPTION, systemNumber,
                0x01.toByte(), // Number of service codes
                (serviceCode and 0xff).toByte(), // Service code (lower byte)
                (serviceCode shr 8).toByte(), // Service code (upper byte)
                blockNumbers.size.toByte(), // Number of blocks to read
                *blockRequests
        )

        if (resp.byteArrayToInt(9, 1) != 0) {
            // Status flag 1!
            Log.w(TAG, "Error reading from card: ${resp.getHexString(9, 2)}")
            return null
        }

        if (resp.size < 12) {
            // Short response
            Log.w(TAG, "Short response from card: expected more than 12 bytes, got ${resp.size}")
            return null
        }

        if (resp.size != 12 + (resp[11].toInt() * 16)) {
            // Short response
            Log.w(TAG, "Bad response from card: expected ${12 + (resp[11].toInt() * 16)} bytes," +
                    " got ${resp.size} ")
            return null
        }

        // Process the response data
        return resp.sliceArray(12 until resp.size).chunked(16).withIndex().associate {
            Pair(blockNumbers[it.index], it.value)
        }
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

        // Reset Mode (s4.4.16)
        private const val COMMAND_RESET_MODE: Byte = 0x3e
        private const val RESPONSE_RESET_MODE: Byte = 0x3f

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

        /**
         * Calculates the IDm for a given system number.
         *
         * @param idm Existing IDm to modify
         * @param systemNumber A system number, up to 4 bits (0x0 - 0xf). If -1, then does not
         * modify the IDm.
         */
        private fun getIdmForSystemNumber(idm: ImmutableByteArray,
                                          systemNumber: Int): ImmutableByteArray {
            return when {
                systemNumber == -1 -> idm

                systemNumber < 0 || systemNumber > 0xf ->
                    throw IllegalArgumentException("systemNumber must be in range 0x0-0xf, or -1")

                else -> ImmutableByteArray.ofB(
                        (idm[0].toInt() and 0xf) or (systemNumber shl 4)) +
                        idm.sliceArray(1 until idm.size)
            }
        }

        /**
         * Determines if two IDm are equal, by ignoring the upper 4 bits of the manufacturer ID.
         *
         * The upper four bits are the system number.
         */
        private fun idmEquals(a: ImmutableByteArray, b: ImmutableByteArray) =
                (a.sliceArray(1 until a.size) == b.sliceArray(1 until b.size)) and
                        ((a[0].toInt() and 0xf) == (b[0].toInt() and 0xf))


        /**
         * Encodes a block number, per s4.2.1 "Block List and Block List Element"
         */
        private fun encodeBlockRequest(blockNumber: Int,
                                       accessMode: Int = 0,
                                       serviceCodeOrder: Int = 0): ImmutableByteArray {
            if (blockNumber < 0 || blockNumber > 0xffff) {
                throw IllegalArgumentException("blockNumber must be 0x0000-0xffff")
            }

            if (accessMode < 0 || accessMode > 0x7) {
                throw IllegalArgumentException("accessMode must be 3 bits")
            }

            if (serviceCodeOrder < 0 || serviceCodeOrder > 0x0f) {
                throw IllegalArgumentException("serviceCodeOrder must be 4 bits")
            }

            val flags = ((accessMode shl 3) or (serviceCodeOrder))

            return if (blockNumber > 0xff) {
                ImmutableByteArray.ofB(flags,
                        (blockNumber and 0xff), // block number, lower byte
                        (blockNumber shr 8))   // block number, upper byte
            } else {
                ImmutableByteArray.ofB(flags or 0x80,
                       blockNumber and 0xff)
            }
        }
    }
}
