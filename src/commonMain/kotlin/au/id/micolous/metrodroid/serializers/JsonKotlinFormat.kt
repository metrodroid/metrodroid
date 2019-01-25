/*
 * JsonKotlinFormat.kt
 *
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

package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import kotlinx.io.OutputStream
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.JSON

class JsonKotlinFormat : CardExporter {
    override fun writeCard(s: OutputStream, card: Card) {
        s.write(writeCard(card).toUtf8Bytes())
    }
    fun writeCard(card: Card) = JSON(indented = true, encodeDefaults = false).stringify(Card.serializer(), card)
    fun readCard(input: String): Card =
            JSON.parse(Card.serializer(), input)
}

// Standard polymorphic serializer works fine but let's avoid putting
// full class names in stored formats
abstract class MultiTypeSerializer<T : Any> : KSerializer<T> {
    abstract val name: String
    override val descriptor: SerialDescriptor
    get() = object : SerialClassDescImpl(name) {
        override val kind: SerialKind = UnionKind.POLYMORPHIC

        init {
            addElement("type")
            addElement("contents")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun serialize(output: Encoder, obj: T) {
        @Suppress("NAME_SHADOWING")
        val output = output.beginStructure(descriptor)
        val (str, serializer) = obj2serializer(obj)
        output.encodeStringElement(descriptor, 0, str)
        output.encodeSerializableElement(descriptor, 1, serializer as KSerializer<T>,
                obj)
        output.endStructure(descriptor)
    }

    abstract fun obj2serializer(obj: T): Pair<String, KSerializer<out T>>
    abstract fun str2serializer(name: String): KSerializer<out T>

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(input: Decoder): T {
        @Suppress("NAME_SHADOWING")
        val input = input.beginStructure(descriptor)
        var klassName: String? = null
        var value: T? = null
        mainLoop@ while (true) {
            when (input.decodeElementIndex(descriptor)) {
                READ_ALL -> {
                    klassName = input.decodeStringElement(descriptor, 0)
                    val loader = str2serializer(klassName) as KSerializer<T>
                    value = input.decodeSerializableElement(descriptor, 1, loader)
                    break@mainLoop
                }
                READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = input.decodeStringElement(descriptor, 0)
                }
                1 -> {
                    val loader = str2serializer(klassName!!) as KSerializer<T>
                    value = input.decodeSerializableElement(descriptor, 1, loader)
                }
                else -> throw SerializationException("Invalid index")
            }
        }

        input.endStructure(descriptor)
        return value!!
    }
}