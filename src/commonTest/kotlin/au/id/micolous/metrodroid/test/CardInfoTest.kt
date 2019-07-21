package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.transit.CardInfoRegistry
import kotlin.test.Test
import kotlin.test.assertNotNull

class CardInfoTest : BaseInstrumentedTest() {
    @Test
    fun testCardInfo() {
        CardInfoRegistry.allCardsAlphabetical.forEach {
            assertNotNull(it)
            assertNotNull(it.name)
            assertNotNull(it.cardType)
        }
    }
}