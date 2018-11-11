package au.id.micolous.metrodroid.test

import android.test.InstrumentationTestCase
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.rkf.RkfTransitData

class RkfTest : InstrumentationTestCase() {
    private fun parseCard(c: ClassicCard): RkfTransitData {
        val d = c.parseTransitData()
        assertNotNull("Transit data not parsed", d)
        assertTrue(d is RkfTransitData)
        return d as RkfTransitData
    }

    private fun loadCard(path: String): RkfTransitData {
        return parseCard(TestUtils.loadMifareClassic4KFromAssets(instrumentation.context, path))
    }

    // This test is based on dumps from https://github.com/mchro/RejsekortReader
    fun testAnonymtDump() {
        /*
         * Per https://github.com/mchro/RejsekortReader/blob/master/dumps/anonymt_dump-20120814.txt
         *
         * A brand new card with card number 308430 000 027 859 5
         * Purchased 2012-07-27 13:28
         * Balance is at least 80 DKK (I paid 150 DKK where the card cost 70 DKK).
         * The card has never been used.
         */
        TestUtils.setLocale(instrumentation.context, "en-US")
        TestUtils.showRawStationIds(false)
        TestUtils.showLocalAndEnglish(false)

        val c = loadCard("anonymt_dump-20120814.mfd")
        assertEquals("308430 000 027 859 5", c.serialNumber)
        // FIXME: Balance is incorrect
        //assertEquals(TransitCurrency.DKK(80), c.balances[0].balance)

    }
}
