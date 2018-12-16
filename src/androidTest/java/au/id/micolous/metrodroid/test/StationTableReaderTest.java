package au.id.micolous.metrodroid.test;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransaction;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoData;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTrip;
import au.id.micolous.metrodroid.transit.suica.SuicaDBUtil;
import au.id.micolous.metrodroid.util.StationTableReader;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

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

    private final int SHINJUKU_REGION_CODE = 0;
    private final int SHINJUKU_LINE_CODE = 37;
    private final int SHINJUKU_STATION_CODE = 10;

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

    private final int EASYCARD_BR02 = 0x12;
    private final int EASYCARD_BR19 = 0x1a;
    private final int EASYCARD_BL23_BR24 = 0x1f;
    private final int EASYCARD_BL12_R10 = 0x33;

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
}
