package au.id.micolous.metrodroid.test;

import android.test.AndroidTestCase;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoData;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTrip;
import au.id.micolous.metrodroid.transit.suica.SuicaDBUtil;
import au.id.micolous.metrodroid.util.StationTableReader;

/**
 * Tests StationTableReader (MdST). This uses the SEQ Go stop database.
 */

public class StationTableReaderTest extends AndroidTestCase {
    public void testSeqGoDatabase() {
        TestUtils.setLocale(getContext(), "en-US");
        TestUtils.showRawStationIds(false);

        Station s = StationTableReader.getStation(SeqGoData.SEQ_GO_STR, SeqGoTrip.DOMESTIC_AIRPORT);
        assertEquals("Domestic Airport", s.getStationName());

        // Try when Raw Station IDs are enabled.
        TestUtils.showRawStationIds(true);
        s = StationTableReader.getStation(SeqGoData.SEQ_GO_STR, SeqGoTrip.DOMESTIC_AIRPORT);
        assertEquals("Domestic Airport [0x9]", s.getStationName());

        // Reset back to default
        TestUtils.showRawStationIds(false);
    }

    public void testLicenseNotice() {
        String notice = StationTableReader.getNotice(SeqGoData.SEQ_GO_STR);
        assertNotNull(notice);
        assertTrue(notice.contains("Translink"));
    }

    private final int SHINJUKU_REGION_CODE = 0;
    private final int SHINJUKU_LINE_CODE = 37;
    private final int SHINJUKU_STATION_CODE = 10;

    public void testSuicaDatabase() {
        // Suica has localised station names. Make sure these come out correctly
        TestUtils.setLocale(getContext(), "en-US");
        TestUtils.showRawStationIds(false);
        TestUtils.showLocalAndEnglish(false);

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
        TestUtils.setLocale(getContext(), "ja-JP");
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE);
        assertNotNull(s);
        assertEquals("東日本旅客鉄道", s.getCompanyName());
        assertEquals("新宿", s.getStationName());
        assertEquals(1, s.getLineNames().size());
        assertEquals("山手", s.getLineNames().get(0));

        // Test in another supported language. We should fall back to English here.
        TestUtils.setLocale(getContext(), "fr-FR");
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE);
        assertNotNull(s);
        assertEquals("JR East", s.getCompanyName());
        assertEquals("Shinjuku", s.getStationName());
        // FIXME: We currently have incorrect romanisation for the Yamanote line (Yamate), so just
        // check that this is not the Japanese name.
        assertEquals(1, s.getLineNames().size());
        assertFalse(s.getLineNames().get(0).equalsIgnoreCase("山手"));

        // Test showing both English and Japanese strings
        TestUtils.setLocale(getContext(), "en-US");
        TestUtils.showLocalAndEnglish(true);
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE);
        assertNotNull(s);
        assertEquals("Shinjuku (新宿)", s.getStationName());

        // Test showing both Japanese and English strings.
        TestUtils.setLocale(getContext(), "ja-JP");
        s = SuicaDBUtil.getRailStation(SHINJUKU_REGION_CODE, SHINJUKU_LINE_CODE, SHINJUKU_STATION_CODE);
        assertNotNull(s);
        assertEquals("新宿 (Shinjuku)", s.getStationName());
    }
}
