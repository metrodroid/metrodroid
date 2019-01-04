/*
 * ClassicStaticKeys.java
 *
 * Copyright 2018 Google
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

import android.content.Context
import android.database.Cursor
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.provider.KeysTableColumns
import au.id.micolous.metrodroid.util.Utils
import org.json.JSONException
import org.json.JSONObject

/**
 * Helper for access to static MIFARE Classic keys. This can be used for keys that should be
 * attempted on multiple cards.
 *
 * This is only really useful when a transit agency doesn't implement key diversification.
 *
 * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys#static-json for
 * a file format description.
 */
class ClassicStaticKeys private constructor(override val description: String?,
                                            keys: Map<Int, List<ClassicSectorKey>>,
                                            override val sourceDataLength: Int)
    : ClassicKeysImpl(mKeys = keys) {

    private operator fun plus(other: ClassicStaticKeys): ClassicStaticKeys {
        return ClassicStaticKeys(description = description,
                keys = ClassicKeysImpl.flattenKeys(listOf(this, other)),
                sourceDataLength = sourceDataLength + other.sourceDataLength)
    }

    @Throws(JSONException::class)
    override fun toJSON(): JSONObject {
        val json = baseJson
        if (description != null)
            json.put(JSON_TAG_ID_DESC, description)
        return json
    }

    override val type = CardKeys.TYPE_MFC_STATIC

    override val uid = CardKeys.CLASSIC_STATIC_TAG_ID

    override val fileType: String
        get() = Utils.localizePlural(R.plurals.keytype_mfc_static, keyCount, keyCount)

    companion object {
        private const val JSON_TAG_ID_DESC = "Description"

        /**
         * Retrieves all statically defined MIFARE Classic keys.
         * @return All [ClassicCardKeys], or null if not found
         */
        fun forStaticClassic(context: Context, retriever: CardKeysRetriever): ClassicStaticKeys? {
            val cur = retriever.makeStaticClassicCursor(context) ?: return fallback()
            return fromCursor(cur)
        }

        private fun fromCursor(cursor: Cursor): ClassicStaticKeys? {
            var keys: ClassicStaticKeys? = null

            // Static key requests should give all of the static keys.
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE)) != CardKeys.TYPE_MFC_STATIC)
                    continue
                try {
                    val id = cursor.getInt(cursor.getColumnIndex(KeysTableColumns._ID))
                    val json = JSONObject(cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA)))
                    val nk = ClassicStaticKeys.fromJSON(json, "cursor/$id") ?: continue
                    if (keys == null)
                        keys = nk
                    else
                        keys += nk
                } catch (ignored: JSONException) {
                }
            }
            return keys
        }

        fun fallback() = ClassicStaticKeys(description = "fallback",
                keys = mapOf(), sourceDataLength = 0)

        @Throws(JSONException::class)
        fun fromJSON(jsonRoot: JSONObject, defaultBundle: String) = try {
            ClassicStaticKeys(
                    description = jsonRoot.optString(JSON_TAG_ID_DESC),
                    keys = ClassicKeysImpl.keysFromJSON(jsonRoot, false, defaultBundle),
                    sourceDataLength = jsonRoot.toString().length)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
