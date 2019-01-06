/*
 * ClassicSectorKey.kt
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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


import android.support.annotation.StringRes
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import au.id.micolous.metrodroid.xml.toImmutable
import org.jetbrains.annotations.NonNls
import org.json.JSONException
import org.json.JSONObject
import java.util.*

data class ClassicSectorKey internal constructor(
        var type: KeyType,
        val bundle: String,
        private val mKey: ImmutableByteArray) : Comparable<ClassicSectorKey> {

    val key: ImmutableByteArray
        get() = mKey

    enum class KeyType {
        UNKNOWN,
        A,
        B,
        MULTIPLE;

        val formatRes: Int
            @StringRes
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

        class Transform : org.simpleframework.xml.transform.Transform<KeyType> {
            override fun read(value: String): KeyType {
                return fromString(value)
            }

            @NonNls
            override fun write(value: KeyType): String {
                return value.toString()
            }
        }

        companion object {
            fun fromString(@NonNls keyType: String): KeyType {
                return if (keyType == TYPE_KEYB) {
                    B
                } else {
                    A
                }
            }
        }
    }

    @Throws(JSONException::class)
    fun toJSON(): JSONObject {
        val json = JSONObject()
        when (type) {
            ClassicSectorKey.KeyType.A -> json.put(KEY_TYPE, TYPE_KEYA)
            ClassicSectorKey.KeyType.B -> json.put(KEY_TYPE, TYPE_KEYB)
            else -> {}
        }

        json.put(KEY_VALUE, key.toHexString())

        return json
    }

    override fun compareTo(other: ClassicSectorKey): Int {
        val d = type.compareTo(other.type)
        if (d != 0) return d

        return mKey.compareTo(other.mKey)
    }

    fun invertType() = updateType(keyType = type.inverse())

    fun canonType() = updateType(keyType = type.canon())

    fun updateType(keyType: KeyType) = ClassicSectorKey(mKey = mKey,
            type = keyType, bundle = bundle)

    companion object {
        @NonNls
        private val TYPE_KEYA = "KeyA"
        @NonNls
        private val TYPE_KEYB = "KeyB"

        @NonNls
        private val KEY_TYPE = "type"
        @NonNls
        private val KEY_VALUE = "key"
        @NonNls
        private val KEY_BUNDLE = "bundle"
        internal const val KEY_LEN = 6

        fun fromDump(b: ImmutableByteArray, type: KeyType, bundle: String): ClassicSectorKey {
            if (b.size != KEY_LEN) {
                throw IllegalArgumentException(
                        String.format(Locale.ENGLISH, "Key data must be %d bytes, got %d", KEY_LEN, b.size))
            }

            return ClassicSectorKey(mKey = b,
                    type = type, bundle = bundle)
        }

        fun fromDump(b: ByteArray, offset: Int, type: KeyType, bundle: String) =
                fromDump(b.toImmutable().sliceOffLen(offset, ClassicSectorKey.KEY_LEN),
                        type, bundle)

        @Throws(JSONException::class)
        fun fromJSON(json: JSONObject, defaultBundle: String): ClassicSectorKey {
            val t = if (json.has(KEY_TYPE) && !json.isNull(KEY_TYPE) && !json.getString(KEY_TYPE).isEmpty()) {
                json.getString(KEY_TYPE)
            } else
                null

            val kt = when (t) {
                null -> KeyType.UNKNOWN
                TYPE_KEYA -> KeyType.A
                TYPE_KEYB -> KeyType.B
                else -> KeyType.UNKNOWN
            }

            val keyData = ImmutableByteArray.fromHex(json.getString(KEY_VALUE))

            // Check that the key is the correct length
            if (keyData.size != KEY_LEN) {
                throw JSONException(
                        String.format(Locale.ENGLISH, "Expected %d bytes in key, got %d",
                                KEY_LEN, keyData.size
                        ))
            }

            // Checks completed, pass the data back.
            return ClassicSectorKey(type = kt, mKey = keyData,
                    bundle = json.optString(KEY_BUNDLE, defaultBundle))
        }
    }
}
