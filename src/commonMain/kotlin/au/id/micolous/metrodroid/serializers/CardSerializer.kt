package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.NativeThrows
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

    private fun fromJson(xml: String): Card {
        try {
            return jsonKotlinFormat.readCard(xml)
        } catch (ex: Exception) {
            Log.e("Card", "Failed to deserialize", ex)
            throw RuntimeException(ex)
        }
    }

    @NativeThrows
    fun toJson(card: Card): String {
        try {
            return jsonKotlinFormat.writeCard(card)
        } catch (ex: Exception) {
            Log.e("Card", "Failed to serialize", ex)
            throw RuntimeException(ex)
        }
    }

    @NativeThrows
    fun fromPersist(input: String): Card = fromJson(input)

    @NativeThrows
    fun toPersist(card: Card): String = toJson(card)
}
