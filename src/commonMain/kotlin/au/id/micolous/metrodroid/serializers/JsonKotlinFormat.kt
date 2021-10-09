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
import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.Output
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal val JsonElement.jsonPrimitiveOrNull: JsonPrimitive?
    get() = this as? JsonPrimitive

internal val JsonElement.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

object JsonKotlinFormat : CardExporter, CardImporter {
    override fun writeCard(s: Output, card: Card) {
        val b = makeCardString(card).encodeToByteArray()
        s.write(b)
    }

    private val jsonOutputFormat = Json {
        prettyPrint = true
        encodeDefaults = false
    }

    fun makeCardElement(card: Card) = jsonOutputFormat.encodeToJsonElement(Card.serializer(), card)
    fun makeCardString(card: Card) = jsonOutputFormat.encodeToString(Card.serializer(), card)

    override fun readCard(stream: Input) =
            readCard(stream.readToString())

    // This intentionally runs in non-strict mode.
    //
    // This allows us to skip unknown fields:
    // 1. This lets us remove old fields, without keeping attributes hanging around. There doesn't
    //    seem to be a simple way to explicitly ignore single fields in JSON inputs.
    // 2. Dumps from a newer version of Metrodroid can still be read (though, without these fields).
    val nonstrict = Json {
        useArrayPolymorphism = true
        isLenient = true
        ignoreUnknownKeys = true
    }
    override fun readCard(input: String): Card =
            nonstrict.decodeFromString(Card.serializer(), input)

    fun readCard(input: JsonElement): Card =
        nonstrict.decodeFromJsonElement(Card.serializer(), input)
}

// Standard polymorphic serializer works fine but let's avoid putting
// full class names in stored formats
abstract class MultiTypeSerializer<T> : KSerializer<T> {
    abstract val name: String
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor
    get() = buildSerialDescriptor(
        serialName = name,
        kind = PolymorphicKind.OPEN,
        typeParameters = arrayOf(
            buildSerialDescriptor(serialName = "type",
                kind = PrimitiveKind.STRING),
            buildClassSerialDescriptor(serialName = "contents")
        )
    )

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: T) {
        @Suppress("NAME_SHADOWING")
        val output = encoder.beginStructure(descriptor)
        val (str, serializer) = obj2serializer(value)
        output.encodeStringElement(descriptor, 0, str)
        output.encodeSerializableElement(descriptor, 1, serializer as KSerializer<T>,
                value)
        output.endStructure(descriptor)
    }

    abstract fun obj2serializer(obj: T): Pair<String, KSerializer<out T>>
    abstract fun str2serializer(name: String): KSerializer<out T>

    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        @Suppress("NAME_SHADOWING")
        val input = decoder.beginStructure(descriptor)
        if (input.decodeSequentially()) {
            val klassName = input.decodeStringElement(descriptor, 0)
            val loader = str2serializer(klassName) as KSerializer<T>
            val value = input.decodeSerializableElement(descriptor, 1, loader)
            input.endStructure(descriptor)
            return value
        }

        var klassName: String? = null
        var value: T? = null
        mainLoop@ while (true) {
            when (input.decodeElementIndex(descriptor)) {
                DECODE_DONE -> {
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
