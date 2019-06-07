/*
 * ClassicSectorKey.kt
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2019 Google
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

import au.id.micolous.metrodroid.key.ClassicKeysImpl.Companion.TRANSFORM_KEY
import au.id.micolous.metrodroid.key.ClassicSectorKey.Companion.KEY_TYPE
import au.id.micolous.metrodroid.key.ClassicSectorKey.Companion.KEY_VALUE
import au.id.micolous.metrodroid.key.ClassicSectorKey.Companion.SECTOR_IDX
import au.id.micolous.metrodroid.key.ClassicSectorKey.Companion.TYPE_KEYA
import au.id.micolous.metrodroid.key.ClassicSectorKey.Companion.TYPE_KEYB
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.json
import kotlin.experimental.xor

interface ClassicSectorAlgoKey {
    fun resolve(tagId: ImmutableByteArray, sector: Int): ClassicSectorKey
    fun toJSON(sector: Int): JsonObject
    val bundle: String
}

data class TouchnGoKey (val key: ImmutableByteArray,
                        val type: ClassicSectorKey.KeyType): ClassicSectorAlgoKey {
    override fun toJSON(sector: Int): JsonObject = json {
        when (type) {
            ClassicSectorKey.KeyType.A -> KEY_TYPE to TYPE_KEYA
            ClassicSectorKey.KeyType.B -> KEY_TYPE to TYPE_KEYB
            else -> {
            }
        }
        KEY_VALUE to key.toHexString()
        SECTOR_IDX to sector
        TRANSFORM_KEY to "touchngo"
    }

    override fun resolve(tagId: ImmutableByteArray, sector: Int): ClassicSectorKey {
        val pattern = ImmutableByteArray.of(
                tagId[1] xor tagId[2] xor tagId[3],
                tagId[1],
                tagId[2],
                (tagId[0] + tagId[1] + tagId[2] + tagId[3]).toByte() xor tagId[3],
                0,
                0
        )
        return ClassicSectorKey(
                type = type,
                key = key xor pattern,
                bundle = bundle
        )
    }

    override val bundle: String
        get() = "touchngo"
}

@Serializable
data class ClassicSectorKey internal constructor(
        var type: KeyType,
        override val bundle: String,
        val key: ImmutableByteArray) : Comparable<ClassicSectorKey>, ClassicSectorAlgoKey {
    override fun resolve(tagId: ImmutableByteArray, sector: Int): ClassicSectorKey = this

    enum class KeyType {
        UNKNOWN,
        A,
        B,
        MULTIPLE;

        val formatRes: StringResource
            get() = when (this) {
                A -> R.string.classic_key_format_a
                B -> R.string.classic_key_format_b
                else -> R.string.classic_key_format
            }

        fun inverse() = if (this == B) {
            A
        } else {
            B
        }

        fun canon() = if (this == B) {
            B
        } else {
            A
        }

        override fun toString() = if (this == B) {
            TYPE_KEYB
        } else {
            TYPE_KEYA
        }
    }

    override fun compareTo(other: ClassicSectorKey): Int {
        val d = type.compareTo(other.type)
        if (d != 0) return d

        return key.compareTo(other.key)
    }

    fun invertType() = updateType(keyType = type.inverse())

    fun canonType() = updateType(keyType = type.canon())

    fun updateType(keyType: KeyType) = ClassicSectorKey(key = key,
            type = keyType, bundle = bundle)

    override fun toJSON(sector: Int): JsonObject = json {
            when (type) {
                ClassicSectorKey.KeyType.A -> KEY_TYPE to TYPE_KEYA
                ClassicSectorKey.KeyType.B -> KEY_TYPE to TYPE_KEYB
                else -> {
                }
            }
            KEY_VALUE to key.toHexString()
            SECTOR_IDX to sector
        }

    companion object {
        internal const val KEY_LEN = 6
        const val KEY_TYPE = "type"
        const val KEY_VALUE = "key"
        const val SECTOR_IDX = "sector"
        const val TYPE_KEYA = "KeyA"
        const val TYPE_KEYB = "KeyB"


        fun fromDump(b: ImmutableByteArray, type: KeyType, bundle: String): ClassicSectorKey {
            if (b.size != KEY_LEN) {
                throw IllegalArgumentException("Key data must be $KEY_LEN bytes, got ${b.size}")
            }

            return ClassicSectorKey(key = b,
                    type = type, bundle = bundle)
        }

        fun fromDump(b: ImmutableByteArray, offset: Int, type: KeyType, bundle: String) =
                fromDump(b.sliceOffLen(offset, ClassicSectorKey.KEY_LEN),
                        type, bundle)
    }
}
