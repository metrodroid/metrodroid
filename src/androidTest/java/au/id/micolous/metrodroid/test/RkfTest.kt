package au.id.micolous.metrodroid.test

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.MfcCardImporter
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.rkf.RkfTransitData
import java.util.*

class RkfTest : CardReaderWithAssetDumpsTest<RkfTransitData, ClassicCard>(
        RkfTransitData::class.java, MfcCardImporter()) {

    // This test is based on dumps from https://github.com/mchro/RejsekortReader
    @Test
    fun testAnonymtDump() {
        /*
         * Per https://github.com/mchro/RejsekortReader/blob/master/dumps/anonymt_dump-20120814.txt
         *
         * A brand new card with card number 308430 000 027 859 5
         * Purchased 2012-07-27 13:28
         * Balance is at least 80 DKK (I paid 150 DKK where the card cost 70 DKK).
         * The card has never been used.
         */
        setLocale("en-US")
        showRawStationIds(false)
        showLocalAndEnglish(false)

        val c = loadAndParseCard("anonymt_dump-20120814.mfd")
        assertEquals("308430 000 027 859 5", c.serialNumber)
        assertEquals(TransitCurrency.DKK(100 * 100), c.balances[0].balance)
        assertEquals(TimeZone.getTimeZone("Europe/Copenhagen"), c.trips[0].startTimestamp.timeZone)
        assertEquals("Rejsekort", c.issuer)
        //assertEquals("2014-12-31", Utils.isoDateFormat(c.expiryDate!!))
        assertEquals(R.string.rkf_status_action_pending, c.cardStatus)
    }
}
