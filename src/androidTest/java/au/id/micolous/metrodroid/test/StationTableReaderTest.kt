package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.*

import au.id.micolous.metrodroid.transit.adelaide.AdelaideTransaction
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransaction
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed
import au.id.micolous.metrodroid.transit.seq_go.SeqGoData
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTrip
import au.id.micolous.metrodroid.transit.suica.SuicaDBUtil
import au.id.micolous.metrodroid.util.StationTableReader

import au.id.micolous.metrodroid.transit.en1545.En1545Transaction.Companion.TRANSPORT_BUS
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction.Companion.TRANSPORT_METRO
import kotlin.test.*

/**
 * Tests StationTableReader (MdST). This uses the SEQ Go stop database.
 */
class StationTableReaderTest : BaseInstrumentedTest() {
    @Test
    fun testSeqGoDatabase() {
        setLocale("en-US")
        showRawStationIds(false)

        var s = StationTableReader.getStation(SeqGoData.SEQ_GO_STR, SeqGoData.DOMESTIC_AIRPORT)
        assertEquals("Domestic Airport", s.getStationName(false))

        // Try when Raw Station IDs are enabled.
        showRawStationIds(true)
        s = StationTableReader.getStation(SeqGoData.SEQ_GO_STR, SeqGoData.DOMESTIC_AIRPORT)
        assertEquals("Domestic Airport [0x9]", s.getStationName(false))

        // Reset back to default
        showRawStationIds(false)
    }

    @Test
    fun testLicenseNotice() {
        val notice = StationTableReader.getNotice(SeqGoData.SEQ_GO_STR)
        assertNotNull(notice)
        assertTrue(notice!!.contains("Translink"))
    }

    @Test
    fun testSuicaDatabase() {
        // Suica has localised station names. Make sure these come out correctly
        setLocale("en-US")
        showRawStationIds(false)
        showLocalAndEnglish(false)

        // Test a station in English
        var s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE)
        assertNotNull(s)
        assertEquals("JR East", s.companyName)
        assertEquals("Shinjuku", s.getStationName(false))
        assertEquals(1, s.lineNames!!.size)
        // FIXME: We currently have incorrect romanisation for the Yamanote line (Yamate), so just
        // check that this is not the Japanese name.
        assertFalse(s.lineNames!![0].equals("山手", ignoreCase = true))

        // Test in Japanese
        setLocale("ja-JP")
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE)
        assertNotNull(s)
        assertEquals("東日本旅客鉄道", s.companyName)
        assertEquals("新宿", s.getStationName(false))
        assertEquals(1, s.lineNames!!.size)
        assertEquals("山手", s.lineNames!![0])

        // Test in another supported language. We should fall back to English here.
        setLocale("fr-FR")
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE)
        assertNotNull(s)
        assertEquals("JR East", s.companyName)
        assertEquals("Shinjuku", s.getStationName(false))
        // FIXME: We currently have incorrect romanisation for the Yamanote line (Yamate), so just
        // check that this is not the Japanese name.
        assertEquals(1, s.lineNames!!.size)
        assertFalse(s.lineNames!![0].equals("山手", ignoreCase = true))

        // Test showing both English and Japanese strings
        setLocale("en-US")
        showLocalAndEnglish(true)
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE)
        assertNotNull(s)
        assertEquals("Shinjuku (新宿)", s.getStationName(false))

        // Test showing both Japanese and English strings.
        setLocale("ja-JP")
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE)
        assertNotNull(s)
        assertEquals("新宿 (Shinjuku)", s.getStationName(false))
    }

    private fun createEasyCardTrip(startStation: Int, endStation: Int): Trip {
        val start = EasyCardTransaction(
                0x1234L,
                0,
                startStation,
                false,
                0x6789L
        )

        val end = EasyCardTransaction(
                0x2345L,
                10,
                endStation,
                true,
                0xabcd
        )

        val trips = TransactionTrip.merge(listOf(start, end))
        assertEquals(1, trips.size)
        return trips[0]
    }

    @Test
    fun testEasyCardLineSelection() {
        setLocale("en-US")
        showRawStationIds(false)
        showLocalAndEnglish(false)

        var trip: Trip = createEasyCardTrip(EASYCARD_BR02, EASYCARD_BR19)

        assertEquals("Brown", trip.routeName)

        trip = createEasyCardTrip(EASYCARD_BR02, EASYCARD_BL23_BR24)
        assertEquals("Brown", trip.routeName)

        trip = createEasyCardTrip(EASYCARD_BL23_BR24, EASYCARD_BR19)
        assertEquals("Brown", trip.routeName)

        trip = createEasyCardTrip(EASYCARD_BL23_BR24, EASYCARD_BL12_R10)
        assertEquals("Blue", trip.routeName)

        trip = createEasyCardTrip(EASYCARD_BR02, EASYCARD_BL12_R10)
        assertEquals("Brown", trip.routeName)
    }

    @Parcelize
    private class MockAdelaideTransaction internal constructor(private val mRouteNumber: Int, override val transport: Int) : AdelaideTransaction(En1545Parsed()) {

        override val routeNumber: Int?
            get() = mRouteNumber

        override val agency: Int?
            get() = 1
    }

    @Test
    fun testAdelaideRouteNaming() {
        setLocale("en-US")
        showRawStationIds(false)
        showLocalAndEnglish(false)

        val txn = MockAdelaideTransaction(0x16f, TRANSPORT_BUS)
        assertEquals(listOf("0x16f"), txn.humanReadableLineIDs)
        assertEquals(listOf("M44"), txn.routeNames)

        val txnUnknown = MockAdelaideTransaction(0xffff, TRANSPORT_METRO)
        assertEquals(listOf("0xffff"), txnUnknown.humanReadableLineIDs)
        assertEquals(listOf("Unknown (0xffff)"), txnUnknown.routeNames)

        // Now check at a TransactionTrip level
        val trips = TransactionTrip.merge(txn)
        assertEquals(1, trips.size)
        val tripsUnknown = TransactionTrip.merge(txnUnknown)
        assertEquals(1, tripsUnknown.size)

        val trip = trips[0]
        assertEquals("M44", Trip.getRouteDisplayName(trip))
        assertEquals("M44", trip.routeName)
        assertEquals("0x16f", trip.humanReadableRouteID)

        val tripUnknown = tripsUnknown[0]
        assertEquals("Unknown (0xffff)", Trip.getRouteDisplayName(tripUnknown))
        assertEquals("Unknown (0xffff)", tripUnknown.routeName)
        assertEquals("0xffff", tripUnknown.humanReadableRouteID)

        // Now test with the settings changed.
        showRawStationIds(true)

        // Display name should change
        assertTrue(Trip.getRouteDisplayName(trip)!!.contains("M44"))
        assertTrue(Trip.getRouteDisplayName(trip)!!.contains("0x16f"))

        // Other names should not.
        assertEquals("M44", trip.routeName)
        assertEquals("0x16f", trip.humanReadableRouteID)

        // Unknown names should stay the same.
        assertEquals("Unknown (0xffff)", Trip.getRouteDisplayName(tripUnknown))
        assertEquals("Unknown (0xffff)", tripUnknown.routeName)
        assertEquals("0xffff", tripUnknown.humanReadableRouteID)
    }

    companion object {
        private const val SHINJUKU_REGION_CODE = 0
        private const val SHINJUKU_LINE_CODE = 37
        private const val SHINJUKU_STATION_CODE = 10

        private const val EASYCARD_BR02 = 0x12
        private const val EASYCARD_BR19 = 0x1a
        private const val EASYCARD_BL23_BR24 = 0x1f
        private const val EASYCARD_BL12_R10 = 0x33
    }
}
