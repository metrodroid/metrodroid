package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.card.iso7816.ISO7816File
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol.*
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector
import kotlin.experimental.and

class VirtualISO7816Card(protected val mCard : ISO7816Card) : CardTransceiver {
    var currentApplication : ISO7816Application? = null
    var currentPath : ISO7816Selector? = null
    var currentFile : ISO7816File? = null

    override fun transceive(data: ByteArray): ByteArray {
        val cls = data[0]
        if (cls != CLASS_ISO7816) {
            return COMMAND_NOT_ALLOWED
        }

        val ins = data[1]
        val p1 = data[2]
        val p2 = data[3]
        val params : ByteArray
        if (data.size >= 6) {
            val paramLength = data[4]
            params = data.sliceArray(5 .. 4 + paramLength)
        } else {
            params = ByteArray(0)
        }
        val retLength = data[4 + params.size]

        if (ins == INSTRUCTION_ISO7816_SELECT) {
            return handleSelect(p1, p2, params, retLength)
        } else if (ins == INSTRUCTION_ISO7816_READ_BINARY) {
            return handleReadBinary(p1, p2, params, retLength)
        } else {
            return COMMAND_NOT_ALLOWED
        }
    }

    fun cd(path: Int) : Boolean {
        if (path == 0) {
            currentPath = null
            currentFile = null
        } else if (currentApplication == null) {
            return false
        } else {
            if (currentPath == null) {
                currentPath = ISO7816Selector.makeSelector(path)
            } else {
                currentPath = currentPath!!.appendPath(path)
            }

            currentFile = currentApplication!!.getFile(currentPath)
        }

        return true
    }

    fun handleSelect(p1 : Byte, p2 : Byte, params : ByteArray, retLength : Byte) : ByteArray {
        if (p1 == SELECT_BY_NAME) {
            // Expected an application identifier
            for (application in mCard.applications) {
                if (application.appName.size < params.size) {
                    continue
                }

                val truncatedName = application.appName.sliceArray(0 .. params.lastIndex)

                if (params.contentEquals(truncatedName)) {
                    // we have an app!
                    currentApplication = application
                    return application.appData + OK
                }
            }

            return FILE_NOT_FOUND
        } else if (p1 == 0.toByte()) {
            // don't support selecting a DF
            if (currentApplication == null) {
                return COMMAND_NOT_ALLOWED
            }

            return FILE_NOT_FOUND
        }

        return COMMAND_NOT_ALLOWED
    }

    fun handleReadBinary(p1 : Byte, p2 : Byte, params : ByteArray, retLength : Byte) : ByteArray {
        if (p1.and(0x80.toByte()) > 0) {
            val ef = p1.and(0x1f.toByte())
            val of = p2.toInt()
            val data = currentApplication!!.getSfiFile(ef.toInt()).binaryData!!
            if (retLength == 0.toByte()) {
                return data.sliceArray(of .. data.lastIndex) + OK
            } else {
                return data.sliceArray(of .. (of + retLength - 1)) + OK
            }
        } else {
            if (p1 != 0.toByte() || p2 != 0.toByte()) {
                if (!cd(p1.toInt().shl(8).or(p1.toInt()))) {
                    return FILE_NOT_FOUND
                }
            }

            // Current file
            if (currentFile == null || currentFile!!.binaryData == null) {
                return FILE_NOT_FOUND
            } else {
                return currentFile!!.binaryData!! + OK
            }


        }
    }

    companion object {
        val COMMAND_NOT_ALLOWED = byteArrayOf(ERROR_COMMAND_NOT_ALLOWED, CNA_NO_CURRENT_EF)
        val FILE_NOT_FOUND = byteArrayOf(ERROR_WRONG_PARAMETERS, WP_FILE_NOT_FOUND)
        val OK = byteArrayOf(STATUS_OK, 0)
    }
}