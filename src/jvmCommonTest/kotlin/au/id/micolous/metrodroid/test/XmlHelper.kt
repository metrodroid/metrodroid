package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.serializers.XmlCardFormat
import java.io.InputStream
import kotlin.test.assertNotNull

expect fun loadAssetStream(path: String): InputStream?

actual fun loadCardXml(path: String): Card {
    val asset = loadAssetStream(path)
    assertNotNull(asset)
    val cards = XmlCardFormat().readCards(asset)
    assertNotNull(cards)
    assert(cards.hasNext())
    return cards.next()
}
