/*
 * VirtualISO7816Card.kt
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
package au.id.micolous.metrodroid.test

import android.util.Log
import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.card.iso7816.ISO7816File
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol.*
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector
import au.id.micolous.metrodroid.util.Utils

/**
 * Implements a virtual card that speaks a subset of ISO7816-4.
 *
 * This is intended as a test fixture.
 */
open class VirtualISO7816Card(private val mCard : ISO7816Card) : CardTransceiver {
    private var currentApplication : ISO7816Application? = null
    private var currentPath : ISO7816Selector? = null
    private var currentFile : ISO7816File? = null
    private var currentRecord = 0

    init {
        // Auto-select CEPAS applications that have no name
        for (app in mCard.applications) {
            if (app.appName == null) {
                currentApplication = app
            }
        }
    }

    override fun transceive(data: ByteArray): ByteArray {
        val cls = data[0]
        if (cls != CLASS_ISO7816) {
            return COMMAND_NOT_ALLOWED
        }

        val ins = data[1]
        val p1 = data[2]
        val p2 = data[3]
        val params = if (data.size >= 6) {
            data.sliceArray(5 .. 4 + data[4])
        } else {
            ByteArray(0)
        }
        val retLength = data[4 + params.size].toInt()

        return when (ins) {
            INSTRUCTION_ISO7816_SELECT -> handleSelect(p1, p2, params, retLength)
            INSTRUCTION_ISO7816_READ_BINARY -> handleReadBinary(p1, p2, params, retLength)
            INSTRUCTION_ISO7816_READ_RECORD -> handleReadRecord(p1, p2, params, retLength)
            else -> COMMAND_NOT_ALLOWED
        }
    }

    protected fun cd(path: ISO7816Selector) : Boolean {
        val app = currentApplication ?: return false
        Log.d(TAG, "cd(${path.formatString()})")

        return if (app.pathExists(path)) {
            currentPath = path
            // currentFile may be null
            currentFile = app.getFile(path)
            currentRecord = 0
            val cf = currentFile?.selector?.formatString() ?: "null"
            Log.d(TAG, "... success! currentFile = $cf")
            true
        } else {
            Log.d(TAG, "... not found!")
            false
        }
    }

    protected fun cd(path: Int) : Boolean {
        val app = currentApplication

        if (path == 0) {
            currentPath = null
            currentFile = null
        } else if (app == null) {
            return false
        } else {
            var p : ISO7816Selector? = currentPath
            while (p != null) {
                if (cd(p.appendPath(path))) {
                    return true
                }

                p = p.parent()
            }

            // p == null
            // Try a bare path
            p = ISO7816Selector.makeSelector(path)
            return cd(p)
        }

        return true
    }

    fun truncateOkResponse(ret : ByteArray?, retLength: Int) : ByteArray {
        return when {
            ret == null -> OK
            (retLength == 0 || retLength > ret.lastIndex) -> ret + OK
            else -> ret.sliceArray(0 until retLength) + OK
        }
    }

    fun handleSelect(p1 : Byte, p2 : Byte, params : ByteArray, retLength : Int) : ByteArray {
        if (p1 == SELECT_BY_NAME) {
            // Expected an application identifier
            for (application in mCard.applications) {
                val appName = application.appName ?: continue
                if (appName.size < params.size) {
                    continue
                }

                val truncatedName = appName.sliceArray(0 .. params.lastIndex)

                if (params.contentEquals(truncatedName)) {
                    // we have an app!
                    currentApplication = application
                    currentFile = null
                    currentRecord = 0
                    return truncateOkResponse(application.appData, retLength)
                }
            }

            return FILE_NOT_FOUND
        } else if (p1 == 0.toByte()) {
            if (currentApplication == null) {
                return COMMAND_NOT_ALLOWED
            }

            if (params.isEmpty() || params.contentEquals(byteArrayOf(0x3f, 0))) {
                // Unselect file (select MF)
                currentPath = null
                currentFile = null
                currentRecord = 0
                return OK
            }

            return if (!cd(Utils.byteArrayToInt(params))) {
                FILE_NOT_FOUND
            } else {
                truncateOkResponse(currentFile?.fci ?: ByteArray(0), retLength)
            }
        }

        return COMMAND_NOT_ALLOWED
    }

    fun handleReadBinary(p1 : Byte, p2 : Byte, params : ByteArray, retLength : Int) : ByteArray {
        val app = currentApplication ?: return COMMAND_NOT_ALLOWED
        val p1i = byteToInt(p1)
        val p2i = byteToInt(p2)

        if ((p1i and 0x80) > 0) {
            val ef = p1i and 0x1f
            val data = app.getSfiFile(ef)?.binaryData ?: return FILE_NOT_FOUND

            return truncateOkResponse(data.sliceArray(p2i.. data.lastIndex), retLength)
        } else {
            Log.d(TAG, "ReadBinary($p1i, $p2i)")
            if (p1i != 0 || p2i != 0) {
                if (!cd((p1i shl 8) or p2i)) {
                    return FILE_NOT_FOUND
                }
            }

            val f = currentFile ?: return FILE_NOT_FOUND // file doesn't exist
            Log.d(TAG, "... current file = ${f.selector?.formatString() ?: "null"}")
            return truncateOkResponse(f.binaryData, retLength)
        }
    }

    fun handleReadRecord(p1: Byte, p2: Byte, params: ByteArray, retLength: Int) : ByteArray {
        val p1i = byteToInt(p1)
        val p2i = byteToInt(p2)
        val app = currentApplication ?: return COMMAND_NOT_ALLOWED

        if ((p2i and 0x04) > 0) {
            // Record number in P1
            val ef = p2i shr 3

            currentRecord = if (p1i != 0) p1i else currentRecord

            val file = if (ef == 0) {
                app.getFile(currentPath)
            } else {
                app.getSfiFile(ef)
            } ?: return FILE_NOT_FOUND

            val data = file.getRecord(currentRecord)?.data ?: return RECORD_NOT_FOUND
            return truncateOkResponse(data, retLength)
        } else {
            // Record identifier in P1 (not supported)
            return COMMAND_NOT_ALLOWED
        }
    }

    companion object {
        val COMMAND_NOT_ALLOWED = byteArrayOf(ERROR_COMMAND_NOT_ALLOWED, CNA_NO_CURRENT_EF)
        val FILE_NOT_FOUND = byteArrayOf(ERROR_WRONG_PARAMETERS, WP_FILE_NOT_FOUND)
        val RECORD_NOT_FOUND = byteArrayOf(ERROR_WRONG_PARAMETERS, WP_RECORD_NOT_FOUND)
        val OK = byteArrayOf(STATUS_OK, 0)
        val TAG = VirtualISO7816Card::class.java.simpleName

        private fun byteToInt(i : Byte) : Int {
            return i.toInt() and 0xff
        }
    }
}