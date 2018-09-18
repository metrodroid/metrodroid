package au.id.micolous.metrodroid.test;

import android.test.AndroidTestCase;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoData;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTrip;
import au.id.micolous.metrodroid.util.StationTableReader;

/**
 * Tests StationTableReader (MdST). This uses the SEQ Go stop database.
 */

public class StationTableReaderTest extends AndroidTestCase {
    public void testSeqGoDatabase() throws Exception {
        TestUtils.setLocale(getContext(), "en-US");
        Station s = StationTableReader.getStation(SeqGoData.SEQ_GO_STR, SeqGoTrip.DOMESTIC_AIRPORT);
        assertEquals("Domestic Airport", s.getStationName());
    }
}
