/*
 * KeyFormat.java
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

package au.id.micolous.metrodroid.key

import au.id.micolous.metrodroid.multi.Log
import kotlinx.io.charsets.Charsets
import kotlinx.serialization.json.Json

/**
 * Used by [Utils.detectKeyFormat] to return the format of a key contained within a
 * file.
 */
enum class KeyFormat {
    /** Format is unknown  */
    UNKNOWN,
    /** Traditional raw (farebotkeys) binary format  */
    RAW_MFC,
    /** JSON format (unspecified)  */
    JSON,
    /** JSON format (MifareClassic, with UID)  */
    JSON_MFC,
    /** JSON format (MifareClassic, without UID)  */
    JSON_MFC_NO_UID,
    /** JSON format (MifareClassicStatic)  */
    JSON_MFC_STATIC;

    val isJSON: Boolean
        get() = (this == KeyFormat.JSON
                || this == KeyFormat.JSON_MFC
                || this == KeyFormat.JSON_MFC_NO_UID
                || this == KeyFormat.JSON_MFC_STATIC)

    companion object {
        const val TAG = "KeyFormat"
        private const val MIFARE_SECTOR_COUNT_MAX = 40
        private const val MIFARE_KEY_LENGTH = 6

        private fun isRawMifareClassicKeyFileLength(length: Int): Boolean {
            return length > 0 &&
                    length % MIFARE_KEY_LENGTH == 0 &&
                    length <= MIFARE_SECTOR_COUNT_MAX * MIFARE_KEY_LENGTH * 2
        }

        private fun rawFormat(length: Int) = if (isRawMifareClassicKeyFileLength(length)) KeyFormat.RAW_MFC else KeyFormat.UNKNOWN

        fun detectKeyFormat(data: ByteArray): KeyFormat {
            if (data[0] != '{'.toByte()) {
                // This isn't a JSON file.
                Log.d(TAG, "couldn't find starting {")
                return rawFormat(data.size)
            }

            // Scan for the } at the end of the file.
            for (i in (data.size - 1) downTo 0) {
                val c = data[i]
                if (c <= 0) {
                    Log.d(TAG, "unsupported encoding at byte $i")
                    return rawFormat(data.size)
                }
                if (c in listOf('\n'.toByte(), '\r'.toByte(), '\t'.toByte(),
                                ' '.toByte())) {
                    continue
                }

                if (c == '}'.toByte()) {
                    break
                }

                // This isn't a JSON file.
                Log.d(TAG, "couldn't find ending }")
                return if (isRawMifareClassicKeyFileLength(data.size)) KeyFormat.RAW_MFC else KeyFormat.UNKNOWN
            }

            // Now see if it actually parses.
            try {
                val o = Json.plain.parseJson(kotlinx.io.core.String(bytes = data,
                        charset = Charsets.UTF_8)).jsonObject
                val type = o.getPrimitiveOrNull(CardKeys.JSON_KEY_TYPE_KEY)?.contentOrNull
                when(type) {
                    CardKeys.TYPE_MFC ->
                        return if (o.getPrimitiveOrNull(CardKeys.JSON_TAG_ID_KEY)?.contentOrNull?.isEmpty() != false) {
                            KeyFormat.JSON_MFC_NO_UID
                        } else {
                            KeyFormat.JSON_MFC
                        }

                    CardKeys.TYPE_MFC_STATIC -> return KeyFormat.JSON_MFC_STATIC
                }

                // Unhandled JSON format
                return KeyFormat.JSON
            } catch (e: Exception) {
                Log.d(TAG, "couldn't parse JSON object in detectKeyFormat", e)
            }

            // Couldn't parse as JSON -- fallback
            return rawFormat(data.size)
        }

    }
}
