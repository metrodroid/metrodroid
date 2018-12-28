package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardImporter
import au.id.micolous.metrodroid.transit.TransitData
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue

/**
 * Base class for building tests that need Assets data:
 *
 * Examples of use:
 *
 * - Java: BilheteUnicoTest
 * - Kotlin: EasyCardTest
 *
 * @param TD A [TransitData] subclass for the transit provider that is expected.
 * @param C A [Card] subclass for the type of media to accept.
 * @param transitDataClass A reference to the [TransitData] implementation, to survive type erasure.
 * @param importer A reference to a [CardImporter] which produces [C].
 */
abstract class CardReaderWithAssetDumpsTest<TD : TransitData, C : Card>(
        private val transitDataClass: Class<TD>,
        private val importer: CardImporter<C>
) : BaseInstrumentedTest() {
    /**
     * Parses a card and checks that it was the correct reader.
     */
    fun parseCard(c: C): TD {
        val d = c.parseTransitData()
        assertNotNull("Transit data not parsed", d)
        return transitDataClass.cast(d)!!
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
    fun loadCard(path: String): C {
        val card = importer.readCard(loadAsset(path))
        assertNotNull(card)
        return card!!
    }

    fun loadAndParseCard(path: String): TD {
        return parseCard(loadCard(path))
    }
}