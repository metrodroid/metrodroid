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

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.json

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

    internal operator fun plus(other: ClassicStaticKeys): ClassicStaticKeys {
        return ClassicStaticKeys(description = description,
                keys = flattenKeys(listOf(this, other)),
                sourceDataLength = sourceDataLength + other.sourceDataLength)
    }

    override fun toJSON(): JsonObject {
        if (description == null)
            return baseJson
        return JsonObject(baseJson + json {
            JSON_TAG_ID_DESC to description
        })
    }

    override val type = CardKeys.TYPE_MFC_STATIC

    override val uid = CardKeys.CLASSIC_STATIC_TAG_ID

    override val fileType: String
        get() = Localizer.localizePlural(R.plurals.keytype_mfc_static, keyCount, keyCount)

    companion object {
        private const val JSON_TAG_ID_DESC = "Description"

        fun fallback() = ClassicStaticKeys(description = "fallback",
                keys = mapOf(), sourceDataLength = 0)

        fun flattenKeys(lst: List<ClassicStaticKeys>): Map<Int, List<ClassicSectorKey>> {
            val keys = mutableMapOf<Int, MutableList<ClassicSectorKey>>()
            for (who in lst)
                for ((key, value) in who.mKeys) {
                    if (!keys.containsKey(key))
                        keys[key] = mutableListOf()
                    keys[key]?.addAll(value)
                }
            return keys
        }

        fun fromJSON(jsonRoot: JsonObject, defaultBundle: String) = try {
            ClassicStaticKeys(
                    description = jsonRoot.getPrimitiveOrNull(JSON_TAG_ID_DESC)?.contentOrNull,
                    keys = ClassicKeysImpl.keysFromJSON(jsonRoot, false, defaultBundle),
                    sourceDataLength = jsonRoot.toString().length)
        } catch (e: Exception) {
            Log.e("ClassicStaticKeys", "parsing failed", e)
            null
        }
    }
}
