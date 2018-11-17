/*
 * OpalTest.java
 *
 * Copyright 2017 Michael Farrell <micolous+git@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.test;

import android.test.AndroidTestCase;

import java.util.Calendar;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.clipper.ClipperTransitData;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Tests for Orca card
 */

public class ClipperTest extends AndroidTestCase {

    // mocked data
    static private final String refill = "000002cfde4400007812345600001388000000000000" +
            "00000000000000000000";
    static private final String trip = "000000040000027600000000de580000de58100000080027000000000000" +
            "006f";
    static private final String testFile0x2 = "0000000000000000000000000000000000007777";
    static private final String testFile0x4 = refill;
    static private final String testFile0x8 = "0022229533";
    static private final String testFile0xe = trip;

    private DesfireCard constructClipperCard() {
        // Construct a card to hold the data.
        DesfireFile f2 = DesfireFile.create(2,
               null, // new RecordDesfireFileSettings((byte)0,(byte)0,null, 48, 5, 5),
                Utils.hexStringToByteArray(testFile0x2));
        DesfireFile f4 = DesfireFile.create(4, null, Utils.hexStringToByteArray(testFile0x4));
        DesfireFile f8 = DesfireFile.create(8,null,
                Utils.hexStringToByteArray(testFile0x8));
        DesfireFile fe = DesfireFile.create(0xe,null,
                Utils.hexStringToByteArray(testFile0xe));
        DesfireApplication a = new DesfireApplication(ClipperTransitData.APP_ID, new DesfireFile[] { f2, f4, f8, fe });
        return new DesfireCard(new byte[] {0, 1, 2, 3},
                Calendar.getInstance(),
                null,
                new DesfireApplication[] { a });
    }

    public void testDemoCard() {
        TestUtils.setLocale(getContext(), "en-US");
        TestUtils.showRawStationIds(false);

        assertEquals(32*2, refill.length());

        // This is mocked-up data, probably has a wrong checksum.
        DesfireCard c = constructClipperCard();

        // Test TransitIdentity
        TransitIdentity i = c.parseTransitIdentity();
        assertEquals(R.string.card_name_sfo_clipper, i.getNameId());
        assertEquals("572691763", i.getSerialNumber());

        TransitData d = c.parseTransitData();
        assertTrue("TransitData must be instance of ClipperTransitData", d instanceof ClipperTransitData);

        ClipperTransitData o = (ClipperTransitData)d;
        assertEquals("572691763", o.getSerialNumber());
        assertEquals(R.string.card_name_sfo_clipper, o.getCardInfo().getNameId());
        assertEquals(TransitCurrency.USD(30583), o.getBalance().getBalance());
        assertNull(o.getSubscriptions());

        List<Trip> trips = o.getTrips();
        assertNotNull(trips);
        assertEquals("Whole Foods", trips.get(1).getAgencyName(false));
        assertEquals("Whole Foods", trips.get(1).getAgencyName(true));
        assertEquals(1520009600000L, trips.get(1).getStartTimestamp().getTimeInMillis());
        assertEquals(TransitCurrency.USD(-5000), trips.get(1).getFare());
        assertNull(trips.get(1).getRouteName());
        assertTrue(trips.get(1).hasTime());
        assertEquals(Trip.Mode.TICKET_MACHINE, trips.get(1).getMode());
        assertNull(trips.get(1).getStartStation());
        assertNull(trips.get(1).getEndStation());
        assertEquals("78123456", trips.get(1).getVehicleID());

        assertEquals("Bay Area Rapid Transit", trips.get(0).getAgencyName(false));
        assertEquals("BART", trips.get(0).getAgencyName(true));
        assertEquals(1521320320000L, trips.get(0).getStartTimestamp().getTimeInMillis());
        assertEquals(TransitCurrency.USD(630), trips.get(0).getFare());
        assertNull(trips.get(0).getRouteName());
        assertTrue(trips.get(0).hasTime());
        assertEquals(Trip.Mode.METRO, trips.get(0).getMode());
        assertNotNull(trips.get(0).getStartStation());
        assertEquals("Powell St.", trips.get(0).getStartStation().getStationName());
        assertEquals("Powell St.", trips.get(0).getStartStation().getShortStationName());
        assertEquals(37.78447, Float.parseFloat(trips.get(0).getStartStation().getLatitude()), 0.00001);
        assertEquals(-122.40797, Float.parseFloat(trips.get(0).getStartStation().getLongitude()), 0.00001);
        assertNotNull(trips.get(0).getEndStation());
        assertEquals("Dublin/Pleasanton", trips.get(0).getEndStation().getStationName());
        assertEquals("Dublin/Pleasanton", trips.get(0).getEndStation().getShortStationName());
        assertEquals(37.70169, Float.parseFloat(trips.get(0).getEndStation().getLatitude()), 0.00001);
        assertEquals(-121.89918, Float.parseFloat(trips.get(0).getEndStation().getLongitude()), 0.00001);
    }
}
