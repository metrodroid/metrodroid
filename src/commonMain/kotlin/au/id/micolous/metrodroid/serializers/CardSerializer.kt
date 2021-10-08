package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.NativeThrows
import au.id.micolous.metrodroid.multi.logAndSwiftWrap
import au.id.micolous.metrodroid.util.Input
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

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

    @NativeThrows
    fun fromAutoJson(json: String): Iterator<Card> = logAndSwiftWrap ("Card", "Failed to deserialize") {
        AutoJsonFormat.readCardList(json).iterator()
    }

    @NativeThrows
    fun toJson(card: Card): String = logAndSwiftWrap ("Card", "Failed to serialize") {
        JsonKotlinFormat.writeCard(card)
    }

    @NativeThrows
    fun fromPersist(input: String): Card = fromJson(input)

    @NativeThrows
    fun toPersist(card: Card): String = toJson(card)

    val jsonPlainStable get() = Json(JsonConfiguration.Stable.copy(useArrayPolymorphism = true))
}
