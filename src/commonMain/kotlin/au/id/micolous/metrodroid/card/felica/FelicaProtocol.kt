/*
 * FelicaProtocol.kt
 *
 * Copyright 2011 Kazzz
 * Copyright 2016-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google Inc
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
 * This code can trace lineage from nfc-felica-lib[0], formerly licensed under
 * Apache License v2.0. The Apache Software Foundation considers the license
 * to be compatible with the GPLv3+[1].
 *
 * As the upstream library[0] appears to be no longer maintained, and we're not
 * aware of other users of Metrodroid's fork[2] of the library, this has been
 * pulled into Metrodroid proper[3]. It has been relicensed as GPLv3+ to be
 * consistent with the remainder of the project.
 *
 * However, since then, this library has been largely re-written in Kotlin.
 *
 * [0]: https://github.com/Kazzz/nfc-felica-lib
 * [1]: https://www.apache.org/licenses/GPL-compatibility.html
 * [2]: https://github.com/metrodroid/nfc-felica-lib
 * [3]: https://github.com/micolous/metrodroid/pull/255
 */
package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.card.*
import au.id.micolous.metrodroid.card.felica.FelicaConsts.COMMAND_POLLING
import au.id.micolous.metrodroid.card.felica.FelicaConsts.COMMAND_READ_WO_ENCRYPTION
import au.id.micolous.metrodroid.card.felica.FelicaConsts.COMMAND_REQUEST_SYSTEMCODE
import au.id.micolous.metrodroid.card.felica.FelicaConsts.COMMAND_RESET_MODE
import au.id.micolous.metrodroid.card.felica.FelicaConsts.COMMAND_SEARCH_SERVICECODE
import au.id.micolous.metrodroid.card.felica.FelicaConsts.SYSTEMCODE_FELICA_LITE
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.toImmutable

/**
 * Protocol implementation for FeliCa and FeliCa Lite.
 *
 * Note: This doesn't implement any of the write commands.
 *
 * This implementation can trace lineage from nfc-felica-lib (Java), but has been pretty much
 * entirely re-written in Kotlin, and de-coupled from Android-specific APIs.
 *
 * FeliCa Card User's Manual, Excerpted Edition:
 * https://www.sony.net/Products/felica/business/tech-support/data/card_usersmanual_2.02.pdf
 * nfcpy
 * https://github.com/nfcpy/nfcpy/blob/master/src/nfc/tag/tt3.py
 * https://github.com/nfcpy/nfcpy/blob/master/src/nfc/tag/tt3_sony.py
 */
class FelicaProtocol(val tag: FelicaTransceiver,
                     /**
                      * IDm (Manufacture Identifier).
                      *
                      * This is the serial number of a FeliCa card.
                      */
                     val idm: ImmutableByteArray) {


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
     * @throws CardLostException If the tag moves out of the NFC field
     * @throws CardTransceiveException On communications errors
     */
    private suspend fun sendRequest(commandCode: Byte, systemNumber: Int?, vararg data: Byte) =
            sendRequest(commandCode, systemNumber, data.toImmutable())

    private suspend fun sendRequest(commandCode: Byte, systemNumber: Int?) =
            sendRequest(commandCode, systemNumber, ImmutableByteArray.empty())

    private suspend fun sendRequest(commandCode: Byte, systemNumber: Int?, vararg data: Number) =
            sendRequest(commandCode, systemNumber, data.toImmutable())

    private suspend fun sendRequest(
            commandCode: Byte, systemNumber: Int?, data: ImmutableByteArray): ImmutableByteArray {
        if (commandCode.toInt() and 0x01 != 0) {
            throw IllegalArgumentException("commandCode must be even")
        }

        val idm = systemNumber?.let { getIdmForSystemNumber(idm, it) } ?: ImmutableByteArray.empty()
        val length = 2 + idm.size + data.size
        val sendBuffer = ImmutableByteArray.ofB(length, commandCode) + idm + data

        if (ENABLE_TRACING) {
            Log.d(TAG, ">>> ${sendBuffer.toHexString()}")
        }

        val recvBuffer = tag.transceive(sendBuffer)

        if (ENABLE_TRACING) {
            Log.d(TAG, "<<< " + recvBuffer.toHexString())
        }

        // Check response command code
        if (commandCode + 1 != recvBuffer[1].toInt()) {
            throw CardTransceiveException("response had unexpected command code")
        }

        // Automatically strip off the length prefix before returning it.
        return recvBuffer.sliceArray(1 until recvBuffer.size)
    }

    /**
     * Gets a list of system codes supported by the card.
     *
     * @throws TagLostException if the tag went out of the field
     */
    suspend fun getSystemCodeList(): IntArray {
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
    suspend fun pollFelicaLite(): Boolean {
        return try {
            pollForSystemCode(SYSTEMCODE_FELICA_LITE) != null
        } catch (e: CardLostException) {
            Log.d(TAG, "Swallowing TagLostExecption", e)
            false
        }
    }

    /**
     * Gets a list of system codes supported by the card, by using Polling.
     *
     * This is required for FeliCa Lite.
     *
     * This is also needed by some very old cards that don't respond to [getSystemCodeList] and
     * [getServiceCodeList]. These have been seen in the wild for Octopus. We also use this on
     * first-generation SZT (FeliCa) cards, because these are similar to Octopus.
     *
     * This whole process isn't reliable, so should only be used for FeliCa Lite and old cards
     * that don't work otherwise!
     *
     * This returns an array with up to 16 elements.  If the card doesn't respond to any of the
     * polls, then this returns an empty array.
     *
     * In the case that the card responds to a poll with non-contiguous system numbers, system code
     * 0 will be inserted. This needs to be skipped when [pollForSystemCode] is called later. This
     * happens if there is some other system on the card that we don't know how to poll for.
     *
     * @param systemCodes An array of systemCodes to Poll for.
     */
    suspend fun pollForSystemCodes(systemCodes: IntArray): IntArray {
        val r = mutableMapOf<Int, Int>()

        for (systemCode in systemCodes) {
            try {
                val id = pollForSystemCode(systemCode)
                if (id != null) {
                    r[id] = systemCode
                }
            } catch (e: CardLostException) {
                Log.d(TAG, "Swallowing CardLostException for system code $systemCode", e)
            }
        }

        if (r.isEmpty()) {
            return intArrayOf()
        }

        // Now we need to turn this map into an array.
        return (0..0xf).map {
            r[it] ?: 0
        }.dropLastWhile {
            // Removes all the zero-entries at the end of the array, while keeping appropriate
            // gaps when the service code increments for something we don't know to poll for.
            it == 0
        }.toIntArray()
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
    suspend fun getServiceCodeList(systemNumber: Int): IntArray {
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
     * Polls for a given system code with the correct IDm.
     *
     * The Polling command selects **any** nearby tag.  This automatically filters out the bad tags.
     *
     * Use [resetMode] to select a system number on a specific tag.  This should only be used for
     * tags that don't respond to [getSystemCodeList].
     *
     * @param systemCode System code to search for, or SYSTEMCODE_ANY to scan for any card.
     * @param maxTries Maximum tries to scan for the card, must be at least 1.
     * @return The system number for the given system code, or null if it doesn't respond to that
     *         system code.
     * @throws CardTransceiveException On communication errors
     * @throws CardLostException If the tag moves out of the field, or there is no response
     */
    suspend fun pollForSystemCode(systemCode: Int, maxTries: Int = 3): Int? {
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
                val i = getSystemNumber(newIdm)
                Log.d(TAG, "Card ${idm.toHexString()} has system code $hexSystemCode at $i")
                return i
            } else {
                Log.d(TAG, "Card ${newIdm.toHexString()} responded to system code $hexSystemCode," +
                        " not ${idm.toHexString()}...")
            }
        }

        throw CardLostException("Tag $idm did not respond to $hexSystemCode after $maxTries " +
                "attempt(s)")
    }

    /**
     * Issues the Reset Mode command, which allows switching a system by its number and IDm.
     *
     * If the card does not support [getSystemCodeList], you may need to use [pollForSystemCode]
     * instead.
     */
    suspend fun resetMode(systemNumber: Int) {
        val res = sendRequest(COMMAND_RESET_MODE, systemNumber,
                0, 0) // Reserved parameter, always specify 0x0000

        if (res.size < 11) {
            // Unexpected response length
            throw CardTransceiveException("Unexpected reset response length (${res.size} != 11)")
        }

        if (res[9] != 0.toByte()) {
            throw CardTransceiveException("Reset response error: ${res.sliceArray(9 until 11).toHexString()}")
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
    private suspend fun searchServiceCode(systemNumber: Int, index: Int): ImmutableByteArray {
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
     * @throws CardLostException if the tag went out of the field
     */
    suspend fun readWithoutEncryption(
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
    suspend fun readWithoutEncryption(
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
        }.flatten().toTypedArray()

        val resp = sendRequest(COMMAND_READ_WO_ENCRYPTION, systemNumber,
                0x01, // Number of service codes
                serviceCode and 0xff, // Service code (lower byte)
                serviceCode shr 8, // Service code (upper byte)
                blockNumbers.size, // Number of blocks to read
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
        const val TAG = "FelicaProtocol"
        const val ENABLE_TRACING = false

        /**
         * Calculates the IDm for a given system number.
         *
         * @param idm Existing IDm to modify
         * @param systemNumber A system number, up to 4 bits (0x0 - 0xf). If -1, then does not
         * modify the IDm.
         */
        fun getIdmForSystemNumber(idm: ImmutableByteArray,
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
        fun idmEquals(a: ImmutableByteArray, b: ImmutableByteArray) =
                (a.sliceArray(1 until a.size) == b.sliceArray(1 until b.size)) and
                        ((a[0].toInt() and 0xf) == (b[0].toInt() and 0xf))

        /**
         * Gets the System Number, which is the upper 4 bits of the Manufacturer Code of the card.
         *
         * This is a 4 bit value.
         */
        fun getSystemNumber(idm: ImmutableByteArray): Int {
            return idm.getBitsFromBuffer(0, 4)
        }

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
