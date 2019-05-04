package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import kotlinx.io.ByteArrayInputStream
import kotlinx.io.InputStream
import kotlinx.serialization.toUtf8Bytes

/**
 * Interface for writing card data importers.
 *
 * By default, this adopts a binary-based ([InputStream]) model. If a character (string) model
 * is desired (such as a format which consists only of plain text), then implement [Text]
 * instead.
 *
 * @param <T> An optional subclass of Card to declare specific format emissions. For example, a
 * format that only supports MIFARE Classic should declare
 * [au.id.micolous.metrodroid.card.classic.ClassicCard] here.
</T> */
interface CardImporter {

    /**
     * Reads cards from the given stream.
     *
     * Implementations should read the file incrementally (lazy), to save memory.
     *
     * By default, this tries to read one card (using [.readCard], and returns a
     * singleton iterator.
     *
     * @param stream Stream to read the card content from.
     */
    fun readCards(stream: InputStream): Iterator<Card>? {
        val card = readCard(stream)
        return if (card == null) {
            null
        } else {
            listOf(card).iterator()
        }
    }

    /**
     * Reads cards from the given String.
     *
     * This method should only be used for data which is already in memory. If loading data from
     * some other source, use [.readCards] instead.
     * @param s String to read from.
     */
    fun readCards(s: String): Iterator<Card>? {
        return readCards(ByteArrayInputStream(s.toUtf8Bytes()))
    }

    /**
     * Reads a single card from the given stream.
     *
     * Implementations should read the card immediately.
     *
     * @param stream Stream to read the card content from.
     */
    fun readCard(stream: InputStream): Card?

    fun readCard(input: String): Card? {
        return readCard(ByteArrayInputStream(input.toUtf8Bytes()))
    }
}
