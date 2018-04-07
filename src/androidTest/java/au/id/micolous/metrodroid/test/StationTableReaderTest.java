package au.id.micolous.metrodroid.test;

import android.test.AndroidTestCase;

import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.proto.Stations;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTrip;
import au.id.micolous.metrodroid.util.StationTableReader;

/**
 * Tests StationTableReader (MdST). This uses the SEQ Go stop database.
 */

public class StationTableReaderTest extends AndroidTestCase {
    public void testSeqGoDatabase() throws Exception {
        TestUtils.setLocale(getContext(), "en-US");
        StationTableReader str = MetrodroidApplication.getInstance().getSeqGoSTR();
        Station s = str.getStationById(SeqGoTrip.DOMESTIC_AIRPORT);
        assertEquals("Domestic Airport", s.getStationName());

        Stations.Station sp = str.getProtoStationById(SeqGoTrip.INTERNATIONAL_AIRPORT);
        assertEquals("International Airport", sp.getEnglishName());
    }
}
