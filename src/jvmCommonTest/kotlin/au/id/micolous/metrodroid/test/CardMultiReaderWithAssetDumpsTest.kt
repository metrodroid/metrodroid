package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardProtocol
import au.id.micolous.metrodroid.serializers.CardImporter
import au.id.micolous.metrodroid.serializers.CardMultiImporter
import au.id.micolous.metrodroid.transit.TransitData
import java.io.InputStream
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

expect fun loadAssetStream(path: String): InputStream?

/**
 * Base class for building tests that need Assets data.
 *
 * @param importer A reference to a [CardMultiImporter].
 */
abstract class CardMultiReaderWithAssetDumpsTest<I : CardMultiImporter>(
    val importer: I
) : BaseInstrumentedTest() {
    /**
     * Parses a card and checks that it was the correct reader.
     */
    inline fun <reified TD: TransitData>parseCard(c: Card): TD {
        val d = c.parseTransitData()
        assertNotNull(d, "Transit data not parsed. Card $c")
        assertTrue(d is TD,
            "Transit data is not of right type")
        return d
    }

    /**
     * Loads a card dump from assets.
     *
     * The non-tests versions of Metrodroid must not contain any of this sort of data. It is only
     * useful for validating publicly published dump files.
     *
     * The preference is to include third-party dumps from git submodules, and then include them
     * with Gradle. Alternatively, files can go into <code>third_party/</code> with a
     * <code>README</code>.
     *
     * @param path Path to the dump, relative to <code>/assets/</code>
     * @return Parsed [C] from the file.
     */
    inline fun <reified C: CardProtocol>loadCard(path: String): Card {
        val asset = loadAssetStream(path)
        assertNotNull(asset)
        val cards = importer.readCards(asset)
        assertNotNull(cards)
        assert(cards.hasNext())
        val card = cards.next()
        assert(!cards.hasNext())
        val protocol = card.allProtocols[0]
        assertTrue(protocol is C)
        return card
    }

    inline fun <reified C: CardProtocol,
            reified TD: TransitData>loadAndParseCard(path: String): TD {
        return parseCard(loadCard<C>(path))
    }
}