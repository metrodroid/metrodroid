package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.logAndSwiftWrap
import au.id.micolous.metrodroid.util.Input
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object CardSerializer {
    fun load(importer: CardImporter, stream: Input): Card? {
        try {
            return importer.readCard(stream)
        } catch (ex: Exception) {
            Log.e("Card", "Failed to deserialize", ex)
            throw RuntimeException(ex)
        }
    }

    private fun fromJson(xml: String): Card = logAndSwiftWrap ("Card", "Failed to deserialize") {
        JsonKotlinFormat.readCard(xml)
    }

    @Throws(Throwable::class)
    @Suppress("unused") // Used from Swift
    fun fromAutoJson(json: String): Iterator<Card> = logAndSwiftWrap ("Card", "Failed to deserialize") {
        AutoJsonFormat.readCardList(json).iterator()
    }

    @Throws(Throwable::class)
    fun toJson(card: Card): JsonElement = logAndSwiftWrap ("Card", "Failed to serialize") {
        JsonKotlinFormat.makeCardElement(card)
    }

    @Throws(Throwable::class)
    fun fromPersist(input: String): Card = fromJson(input)

    @Throws(Throwable::class)
    fun toPersist(card: Card): String = toJson(card).toString()

    val jsonPlainStable get() = Json {
        useArrayPolymorphism = true
        isLenient = true
    }
}
