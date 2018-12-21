package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.XmlCardFormat
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.transit.TransitData
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ISO7816Test : CardReaderWithAssetDumpsTest<TransitData, Card>
    (TransitData::class.java, XmlCardFormat()) {

    @Test
    fun testIso7816Card() {
        val card = loadCard("iso7816/mobib_blank.xml")
        val vcard = VirtualISO7816Card(card as ISO7816Card)

        val feedback = MockFeedbackInterface()
        val rcard = ISO7816Card.dumpTag(vcard, card.tagId, feedback)!!

        assertEquals(card.applications.size, rcard.applications.size)

        val identity = rcard.parseTransitIdentity()
        assertEquals("Mobib", identity.name)
    }
}