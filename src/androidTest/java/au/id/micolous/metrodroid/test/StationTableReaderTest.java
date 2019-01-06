package au.id.micolous.metrodroid.test;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.adelaide.AdelaideTransaction;
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransaction;
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoData;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTrip;
import au.id.micolous.metrodroid.transit.suica.SuicaDBUtil;
import au.id.micolous.metrodroid.util.StationTableReader;

import static au.id.micolous.metrodroid.transit.en1545.En1545Transaction.TRANSPORT_BUS;
import static au.id.micolous.metrodroid.transit.en1545.En1545Transaction.TRANSPORT_METRO;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests StationTableReader (MdST). This uses the SEQ Go stop database.
 */
public class StationTableReaderTest extends BaseInstrumentedTest {
    @Test
    public void testSeqGoDatabase() {
        setLocale("en-US");
        showRawStationIds(false);

        Station s = StationTableReader.getStation(SeqGoData.SEQ_GO_STR, SeqGoTrip.DOMESTIC_AIRPORT);
        assertEquals("Domestic Airport", s.getStationName());

        // Try when Raw Station IDs are enabled.
        showRawStationIds(true);
        s = StationTableReader.getStation(SeqGoData.SEQ_GO_STR, SeqGoTrip.DOMESTIC_AIRPORT);
        assertEquals("Domestic Airport [0x9]", s.getStationName());

        // Reset back to default
        showRawStationIds(false);
    }

    @Test
    public void testLicenseNotice() {
        String notice = StationTableReader.getNotice(SeqGoData.SEQ_GO_STR);
        assertNotNull(notice);
        assertTrue(notice.contains("Translink"));
    }

    private static final int SHINJUKU_REGION_CODE = 0;
    private static final int SHINJUKU_LINE_CODE = 37;
    private static final int SHINJUKU_STATION_CODE = 10;

    @Test
    public void testSuicaDatabase() {
        // Suica has localised station names. Make sure these come out correctly
        setLocale( "en-US");
        showRawStationIds(false);
        showLocalAndEnglish(false);

        // Test a station in English
        Station s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE);
        assertNotNull(s);
        assertEquals("JR East", s.getCompanyName());
        assertEquals("Shinjuku", s.getStationName());
        assertEquals(1, s.getLineNames().size());
        // FIXME: We currently have incorrect romanisation for the Yamanote line (Yamate), so just
        // check that this is not the Japanese name.
        assertFalse(s.getLineNames().get(0).equalsIgnoreCase("山手"));

        // Test in Japanese
        setLocale("ja-JP");
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE);
        assertNotNull(s);
        assertEquals("東日本旅客鉄道", s.getCompanyName());
        assertEquals("新宿", s.getStationName());
        assertEquals(1, s.getLineNames().size());
        assertEquals("山手", s.getLineNames().get(0));

        // Test in another supported language. We should fall back to English here.
        setLocale("fr-FR");
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE);
        assertNotNull(s);
        assertEquals("JR East", s.getCompanyName());
        assertEquals("Shinjuku", s.getStationName());
        // FIXME: We currently have incorrect romanisation for the Yamanote line (Yamate), so just
        // check that this is not the Japanese name.
        assertEquals(1, s.getLineNames().size());
        assertFalse(s.getLineNames().get(0).equalsIgnoreCase("山手"));

        // Test showing both English and Japanese strings
        setLocale("en-US");
        showLocalAndEnglish(true);
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE);
        assertNotNull(s);
        assertEquals("Shinjuku (新宿)", s.getStationName());

        // Test showing both Japanese and English strings.
        setLocale("ja-JP");
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE);
        assertNotNull(s);
        assertEquals("新宿 (Shinjuku)", s.getStationName());
    }

    private static final int EASYCARD_BR02 = 0x12;
    private static final int EASYCARD_BR19 = 0x1a;
    private static final int EASYCARD_BL23_BR24 = 0x1f;
    private static final int EASYCARD_BL12_R10 = 0x33;

    private Trip createEasyCardTrip(int startStation, int endStation) {
        EasyCardTransaction start = new EasyCardTransaction(
                0x1234L,
                0,
                startStation,
                false,
                0x6789L
        );

        EasyCardTransaction end = new EasyCardTransaction(
                0x2345L,
                10,
                endStation,
                true,
                0xabcd
        );

        List<TransactionTrip> trips = TransactionTrip.merge(Arrays.asList(start, end));
        assertEquals(1, trips.size());
        return trips.get(0);
    }

    @Test
    public void testEasyCardLineSelection() {
        setLocale("en-US");
        showRawStationIds(false);
        showLocalAndEnglish(false);

        Trip trip;

        trip = createEasyCardTrip(EASYCARD_BR02, EASYCARD_BR19);
        assertEquals("Brown", trip.getRouteName());

        trip = createEasyCardTrip(EASYCARD_BR02, EASYCARD_BL23_BR24);
        assertEquals("Brown", trip.getRouteName());

        trip = createEasyCardTrip(EASYCARD_BL23_BR24, EASYCARD_BR19);
        assertEquals("Brown", trip.getRouteName());

        trip = createEasyCardTrip(EASYCARD_BL23_BR24, EASYCARD_BL12_R10);
        assertEquals("Blue", trip.getRouteName());

        trip = createEasyCardTrip(EASYCARD_BR02, EASYCARD_BL12_R10);
        assertEquals("Brown", trip.getRouteName());
    }

    private static class MockAdelaideTransaction extends AdelaideTransaction {
        private final int mRouteNumber;
        private final int mTransport;

        MockAdelaideTransaction(int routeNumber, int transport) {
            super(new En1545Parsed());
            mRouteNumber = routeNumber;
            mTransport = transport;
        }

        @Nullable
        @Override
        protected Integer getRouteNumber() {
            return mRouteNumber;
        }

        @Override
        protected int getTransport() {
            return mTransport;
        }

        @Override
        protected Integer getAgency() {
            return 1;
        }
    }

    @Test
    public void testAdelaideRouteNaming() {
        setLocale("en-US");
        showRawStationIds(false);
        showLocalAndEnglish(false);

        final Transaction txn = new MockAdelaideTransaction(0x16f, TRANSPORT_BUS);
        assertEquals(Collections.singletonList("0x16f"), txn.getHumanReadableLineIDs());
        assertEquals(Collections.singletonList("M44"), txn.getRouteNames());

        final Transaction txnUnknown = new MockAdelaideTransaction(0xffff, TRANSPORT_METRO);
        assertEquals(Collections.singletonList("0xffff"), txnUnknown.getHumanReadableLineIDs());
        assertEquals(Collections.singletonList("Unknown (0xffff)"), txnUnknown.getRouteNames());

        // Now check at a TransactionTrip level
        final List<TransactionTrip> trips = TransactionTrip.merge(txn);
        assertEquals(1, trips.size());
        final List<TransactionTrip> tripsUnknown = TransactionTrip.merge(txnUnknown);
        assertEquals(1, tripsUnknown.size());

        final Trip trip = trips.get(0);
        assertEquals("M44", trip.getRouteDisplayName());
        assertEquals("M44", trip.getRouteName());
        assertEquals("0x16f", trip.getHumanReadableRouteID());

        final Trip tripUnknown = tripsUnknown.get(0);
        assertEquals("Unknown (0xffff)", tripUnknown.getRouteDisplayName());
        assertEquals("Unknown (0xffff)", tripUnknown.getRouteName());
        assertEquals("0xffff", tripUnknown.getHumanReadableRouteID());

        // Now test with the settings changed.
        showRawStationIds(true);

        // Display name should change
        assertThat(trip.getRouteDisplayName(), Matchers.containsString("M44"));
        assertThat(trip.getRouteDisplayName(), Matchers.containsString("0x16f"));

        // Other names should not.
        assertEquals("M44", trip.getRouteName());
        assertEquals("0x16f", trip.getHumanReadableRouteID());

        // Unknown names should stay the same.
        assertEquals("Unknown (0xffff)", tripUnknown.getRouteDisplayName());
        assertEquals("Unknown (0xffff)", tripUnknown.getRouteName());
        assertEquals("0xffff", tripUnknown.getHumanReadableRouteID());
    }
}
