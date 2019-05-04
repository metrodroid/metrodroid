package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log
import kotlinx.io.InputStream

object CardSerializer {
    private val jsonKotlinFormat = JsonKotlinFormat()
    private val xmlFormat = XmlCardFormat()

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

    fun fromXmlOrJson(xml: String) = try {
        when (xml.find { it !in listOf('\n', '\r', '\t', ' ') }) {
            '<' -> xmlFormat.readCard(xml)
            '{', '[' -> jsonKotlinFormat.readCard(xml)
            else -> null
        }
    } catch (ex: Exception) {
        Log.e("Card", "Failed to deserialize", ex)
        throw RuntimeException(ex)
    }

    fun toJson(card: Card): String {
        try {
            return jsonKotlinFormat.writeCard(card)
        } catch (ex: Exception) {
            Log.e("Card", "Failed to serialize", ex)
            throw RuntimeException(ex)
        }
    }

    fun fromPersist(input: String): Card = fromJson(input)
    fun toPersist(card: Card): String = toJson(card)
    fun fromDb(data: String): Card? = fromXmlOrJson(data)
}