/*
 * ClassicKeys.java
 *
 * Copyright 2012-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Helper for access to MIFARE Classic keys.
 *
 * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys for details about
 * the data formats implemented here.
 */
class ClassicCardKeys(override var uid: String?,
                      keys: Map<Int, List<ClassicSectorKey>>,
                      override val sourceDataLength: Int) : ClassicKeysImpl(mKeys = keys) {

    val keyType: ClassicSectorKey.KeyType?
        get() {
            val distinct = allKeys.map { it.type }.distinct()
            return when (distinct.size) {
                0 -> null
                1 -> distinct[0]
                else -> ClassicSectorKey.KeyType.MULTIPLE
            }
        }

    override val description
        get() = uid


    /**
     * Serialises this class to JSON format.
     *
     * Must be overridden by subclasses.
     * @return A JSON blob with all the sectors associated with this card.
     */
    @Throws(JSONException::class)
    override fun toJSON(): JSONObject {
        val json = baseJson
        if (uid != null) {
            json.put(CardKeys.JSON_TAG_ID_KEY, uid)
        }
        return json
    }

    fun setAllKeyTypes(kt: ClassicSectorKey.KeyType) {
        mKeys.values.flatten().forEach { it.type = kt }
    }

    override val type = CardKeys.TYPE_MFC

    /**
     * Returns a localised description of the key file type and its contents.
     */
    override val fileType: String
        get() = Localizer.localizePlural(R.plurals.keytype_mfc, keyCount, keyCount)

    companion object {
        /**
         * Reads ClassicCardKeys in "raw" (farebotkeys) format.
         *
         * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys#raw-farebotkeys
         */
        fun fromDump(keyData: ImmutableByteArray) = fromDump(keyData, ClassicSectorKey.KeyType.UNKNOWN)

        fun fromDump(keyData: ImmutableByteArray, keyType: ClassicSectorKey.KeyType): ClassicCardKeys {
            val keys = mutableMapOf<Int, List<ClassicSectorKey>>()

            val numSectors = keyData.size / ClassicSectorKey.KEY_LEN
            for (i in 0 until numSectors) {
                val start = i * ClassicSectorKey.KEY_LEN
                val k = ClassicSectorKey.fromDump(keyData, start, keyType,
                        "from-dump")
                keys[i] = listOf(k)
            }

            return ClassicCardKeys(uid = null, keys = keys, sourceDataLength = keyData.size)
        }

        /**
         * Reads ClassicCardKeys from the internal (JSON) format.
         *
         * See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys#json
         */
        @Throws(JSONException::class)
        fun fromJSON(json: JSONObject, defaultBundle: String) =
            ClassicCardKeys(uid = json.optString(CardKeys.JSON_TAG_ID_KEY),
                    keys = keysFromJSON(json, true, defaultBundle),
                    sourceDataLength = json.toString().length)
    }
}
