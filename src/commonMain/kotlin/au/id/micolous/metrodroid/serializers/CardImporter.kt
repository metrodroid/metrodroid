package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.util.ByteArrayInput
import au.id.micolous.metrodroid.util.Input

/**
 * Interface for writing card data importers.
 *
 * By default, this adopts a binary-based ([InputStream]) model.
 */
interface CardImporter {
    /**
     * Reads a single card from the given stream.
     *
     * Implementations should read the card immediately.
     *
     * @param stream Stream to read the card content from.
     */
    fun readCard(stream: Input): Card?

    fun readCard(input: String): Card?
        = readCard(ByteArrayInput(input.encodeToByteArray()))
}
