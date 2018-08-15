/*
 * OrcaTest.java
 *
 * Copyright 2018 Google
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
import android.util.Base64;

import junit.framework.TestCase;

import java.util.Calendar;

import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile;
import au.id.micolous.metrodroid.card.desfire.settings.RecordDesfireFileSettings;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.orca.OrcaTransitData;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Tests for Orca card
 */

public class OrcaTest extends AndroidTestCase {

    // mocked data
    static private final String record0 = "00000025a4aadc6000076260000000042c00000000000000000000000000" +
            "000000000000000000000000000000000000";
    static private final String record1 = "000000f5a4aacc6000076360000000024200000000000000000000000000" +
            "000000000000000000000000000000000000";
    static private final String record2 = "00000075a4aabc60000338d0000000016600000000000000000000000000" +
            "000000000000000000000000000000000000";
    static private final String record3 = "00000075a4aaac6000000030000000016400000000000000000000000000" +
            "000000000000000000000000000000000000";
    static private final String record4 = "00000085a4aa9c6000027750000000016200000000000000000000000000" +
            "000000000000000000000000000000000000";
    static private final String testFile0x2 = record0 + record1 + record2 + record3 + record4;
    static private final String testFile0x4 = "000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000" +
            "5b88" + "000000000000000000000000000000000000000000";
    static private final String testFile0xf = "0000000000b792a100";

    private DesfireCard constructOrcaCard() {
        // Construct a card to hold the data.
        DesfireFile f2 = RecordDesfireFile.create(2,
                new RecordDesfireFileSettings((byte)0,(byte)0,null, 48, 5, 5),
                Utils.hexStringToByteArray(testFile0x2));
        DesfireFile f4 = DesfireFile.create(4, null, Utils.hexStringToByteArray(testFile0x4));
        DesfireFile ff = DesfireFile.create(15,null,
                Utils.hexStringToByteArray(testFile0xf));
        DesfireApplication a = new DesfireApplication(OrcaTransitData.APP_ID, new DesfireFile[] { f2, f4 });
        DesfireApplication a2 = new DesfireApplication(0xffffff, new DesfireFile[] { ff });
        return new DesfireCard(new byte[] {0, 1, 2, 3},
                Calendar.getInstance(),
                null,
                new DesfireApplication[] { a, a2 });
    }

    public void testDemoCard() {
        TestUtils.setLocale(getContext(), "en-US");

        // This is mocked-up data, probably has a wrong checksum.
        DesfireCard c = constructOrcaCard();

        // Test TransitIdentity
        TransitIdentity i = c.parseTransitIdentity();
        assertEquals("ORCA", i.getName());
        assertEquals("12030625", i.getSerialNumber());

        TransitData d = c.parseTransitData();
        assertTrue("TransitData must be instance of OrcaTransitData", d instanceof OrcaTransitData);

        OrcaTransitData o = (OrcaTransitData)d;
        assertEquals("12030625", o.getSerialNumber());
        assertEquals("ORCA", o.getCardName());
        assertTrue(o.getBalance().equals(TransitCurrency.USD(23432)));
        assertEquals(null, o.getSubscriptions());

        Trip []trips = o.getTrips();
        assertNotNull(trips);
        assertEquals("Community Transit", trips[0].getAgencyName());
        assertEquals("CT", trips[0].getShortAgencyName());
        assertEquals((1514843334L + 256) * 1000, trips[0].getStartTimestamp().getTimeInMillis());
        assertTrue(trips[0].getFare().equals(TransitCurrency.USD(534)));
        assertNull(trips[0].getRouteName());
        assertTrue(trips[0].hasTime());
        assertEquals(Trip.Mode.BUS, trips[0].getMode());
        assertNotNull(trips[0].getStartStation());
        assertNull(trips[0].getEndStation());
        assertEquals("Coach #30246", trips[0].getStartStation().getStationName());

        assertEquals("Unknown (0xf)", trips[1].getAgencyName());
        assertEquals("Unknown (0xf)", trips[1].getShortAgencyName());
        assertEquals(1514843334L * 1000, trips[1].getStartTimestamp().getTimeInMillis());
        assertTrue(trips[1].getFare().equals(TransitCurrency.USD(289)));
        assertNull(trips[1].getRouteName());
        assertTrue(trips[1].hasTime());
        assertEquals(Trip.Mode.BUS, trips[1].getMode());
        assertNotNull(trips[1].getStartStation());
        assertNull(trips[1].getEndStation());
        assertEquals("Coach #30262", trips[1].getStartStation().getStationName());

        assertEquals("Sound Transit", trips[2].getAgencyName());
        assertEquals("ST", trips[2].getShortAgencyName());
        assertEquals((1514843334L-256) * 1000, trips[2].getStartTimestamp().getTimeInMillis());
        assertTrue(trips[2].getFare().equals(TransitCurrency.USD(179)));
        assertEquals("Link Light Rail", trips[2].getRouteName());
        assertTrue(trips[2].hasTime());
        assertEquals(Trip.Mode.METRO, trips[2].getMode());
        assertNotNull(trips[2].getStartStation());
        assertEquals("Stadium Station", trips[2].getStartStation().getStationName());
        assertEquals("Stadium", trips[2].getStartStation().getShortStationName());
        assertEquals(47.5918121, Float.parseFloat(trips[2].getStartStation().getLatitude()), 0.00001);
        assertEquals(-122.327354, Float.parseFloat(trips[2].getStartStation().getLongitude()), 0.00001);
        assertNull(trips[2].getEndStation());

        assertEquals("Sound Transit", trips[3].getAgencyName());
        assertEquals("ST", trips[3].getShortAgencyName());
        assertEquals((1514843334L-512) * 1000, trips[3].getStartTimestamp().getTimeInMillis());
        assertTrue(trips[3].getFare().equals(TransitCurrency.USD(178)));
        assertEquals("Sounder Train", trips[3].getRouteName());
        assertTrue(trips[3].hasTime());
        assertEquals(Trip.Mode.TRAIN, trips[3].getMode());
        assertNotNull(trips[3].getStartStation());
        assertEquals("King Street Station", trips[3].getStartStation().getStationName());
        assertEquals("King Street", trips[3].getStartStation().getShortStationName());
        assertEquals(47.598445, Float.parseFloat(trips[3].getStartStation().getLatitude()), 0.00001);
        assertEquals(-122.330161, Float.parseFloat(trips[3].getStartStation().getLongitude()), 0.00001);
        assertNull(trips[3].getEndStation());

        assertEquals("Washington State Ferries", trips[4].getAgencyName());
        assertEquals("WSF", trips[4].getShortAgencyName());
        assertEquals((1514843334L-768) * 1000, trips[4].getStartTimestamp().getTimeInMillis());
        assertTrue(trips[4].getFare().equals(TransitCurrency.USD(177)));
        assertNull(trips[4].getRouteName());
        assertTrue(trips[4].hasTime());
        assertEquals(Trip.Mode.FERRY, trips[4].getMode());
        assertNotNull(trips[4].getStartStation());
        assertEquals("Seattle Terminal", trips[4].getStartStation().getStationName());
        assertEquals("Seattle", trips[4].getStartStation().getShortStationName());
        assertEquals(47.602722, Float.parseFloat(trips[4].getStartStation().getLatitude()), 0.00001);
        assertEquals(-122.338512, Float.parseFloat(trips[4].getStartStation().getLongitude()), 0.00001);
        assertNull(trips[4].getEndStation());
    }
}
