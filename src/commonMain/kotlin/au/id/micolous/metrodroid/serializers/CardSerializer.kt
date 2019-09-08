package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.NativeThrows
import au.id.micolous.metrodroid.multi.logAndSwiftWrap
import kotlinx.io.InputStream

object CardSerializer {
    private val jsonKotlinFormat = JsonKotlinFormat()

    fun load(importer: CardImporter, stream: InputStream): Card? {
        try {
            return importer.readCard(stream)
        } catch (ex: Exception) {
            Log.e("Card", "Failed to deserialize", ex)
            throw RuntimeException(ex)
        }
    }

    private fun fromJson(xml: String): Card = logAndSwiftWrap ("Card", "Failed to deserialize") {
        jsonKotlinFormat.readCard(xml)
    }

    @NativeThrows
    fun toJson(card: Card): String = logAndSwiftWrap ("Card", "Failed to serialize") {
        jsonKotlinFormat.writeCard(card)
    }

    @NativeThrows
    fun fromPersist(input: String): Card = fromJson(input)

    @NativeThrows
    fun toPersist(card: Card): String = toJson(card)
}
