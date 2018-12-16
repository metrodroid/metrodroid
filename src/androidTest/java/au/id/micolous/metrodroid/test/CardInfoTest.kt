package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.transit.CardInfo
import junit.framework.TestCase.assertNotNull
import org.junit.Test

class CardInfoTest : BaseInstrumentedTest() {
    @Test
    fun testCardInfo() {
        CardInfo.getAllCardsAlphabetical().forEach {
            assertNotNull(it)
            assertNotNull(it.name)
            assertNotNull(it.cardType)
        }
    }
}