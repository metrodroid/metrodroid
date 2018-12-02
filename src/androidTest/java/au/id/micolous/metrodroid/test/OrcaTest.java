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

import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.*;


/**
 * Tests for Orca card
 */

public class OrcaTest extends BaseInstrumentedTest {

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

    @Test
    public void testDemoCard() {
        setLocale("en-US");
        showRawStationIds(false);

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
        assertEquals(TransitCurrency.USD(23432), o.getBalance());
        assertNull(o.getSubscriptions());

        List<Trip> trips = o.getTrips();
        assertNotNull(trips);
        Collections.sort(trips, new Trip.Comparator());
        assertEquals("Community Transit", trips.get(0).getAgencyName(false));
        assertEquals("CT", trips.get(0).getAgencyName(true));
        assertEquals((1514843334L + 256) * 1000, trips.get(0).getStartTimestamp().getTimeInMillis());
        assertEquals(TransitCurrency.USD(534), trips.get(0).getFare());
        assertNull(trips.get(0).getRouteName());
        assertTrue(trips.get(0).hasTime());
        assertEquals(Trip.Mode.BUS, trips.get(0).getMode());
        assertNull(trips.get(0).getStartStation());
        assertNull(trips.get(0).getEndStation());
        assertEquals("30246", trips.get(0).getVehicleID());

        assertEquals("Unknown (0xf)", trips.get(1).getAgencyName(false));
        assertEquals("Unknown (0xf)", trips.get(1).getAgencyName(true));
        assertEquals(1514843334L * 1000, trips.get(1).getStartTimestamp().getTimeInMillis());
        assertEquals(TransitCurrency.USD(289), trips.get(1).getFare());
        assertNull(trips.get(1).getRouteName());
        assertTrue(trips.get(1).hasTime());
        assertEquals(Trip.Mode.BUS, trips.get(1).getMode());
        assertNull(trips.get(1).getStartStation());
        assertNull(trips.get(1).getEndStation());
        assertEquals("30262", trips.get(1).getVehicleID());

        assertEquals("Sound Transit", trips.get(2).getAgencyName(false));
        assertEquals("ST", trips.get(2).getAgencyName(true));
        assertEquals((1514843334L-256) * 1000, trips.get(2).getStartTimestamp().getTimeInMillis());
        assertEquals(TransitCurrency.USD(179), trips.get(2).getFare());
        assertEquals("Link Light Rail", trips.get(2).getRouteName());
        assertTrue(trips.get(2).hasTime());
        assertEquals(Trip.Mode.METRO, trips.get(2).getMode());
        assertNotNull(trips.get(2).getStartStation());
        assertEquals("Stadium", trips.get(2).getStartStation().getStationName());
        assertEquals("Stadium", trips.get(2).getStartStation().getShortStationName());
        assertEquals(47.5918121, Float.parseFloat(trips.get(2).getStartStation().getLatitude()), 0.00001);
        assertEquals(-122.327354, Float.parseFloat(trips.get(2).getStartStation().getLongitude()), 0.00001);
        assertNull(trips.get(2).getEndStation());

        assertEquals("Sound Transit", trips.get(3).getAgencyName(false));
        assertEquals("ST", trips.get(3).getAgencyName(true));
        assertEquals((1514843334L-512) * 1000, trips.get(3).getStartTimestamp().getTimeInMillis());
        assertEquals(TransitCurrency.USD(178), trips.get(3).getFare());
        assertEquals("Sounder Train", trips.get(3).getRouteName());
        assertTrue(trips.get(3).hasTime());
        assertEquals(Trip.Mode.TRAIN, trips.get(3).getMode());
        assertNotNull(trips.get(3).getStartStation());
        assertEquals("King Street", trips.get(3).getStartStation().getStationName());
        assertEquals("King St", trips.get(3).getStartStation().getShortStationName());
        assertEquals(47.598445, Float.parseFloat(trips.get(3).getStartStation().getLatitude()), 0.00001);
        assertEquals(-122.330161, Float.parseFloat(trips.get(3).getStartStation().getLongitude()), 0.00001);
        assertNull(trips.get(3).getEndStation());

        assertEquals("Washington State Ferries", trips.get(4).getAgencyName(false));
        assertEquals("WSF", trips.get(4).getAgencyName(true));
        assertEquals((1514843334L-768) * 1000, trips.get(4).getStartTimestamp().getTimeInMillis());
        assertEquals(TransitCurrency.USD(177), trips.get(4).getFare());
        assertNull(trips.get(4).getRouteName());
        assertTrue(trips.get(4).hasTime());
        assertEquals(Trip.Mode.FERRY, trips.get(4).getMode());
        assertNotNull(trips.get(4).getStartStation());
        assertEquals("Seattle Terminal", trips.get(4).getStartStation().getStationName());
        assertEquals("Seattle", trips.get(4).getStartStation().getShortStationName());
        assertEquals(47.602722, Float.parseFloat(trips.get(4).getStartStation().getLatitude()), 0.00001);
        assertEquals(-122.338512, Float.parseFloat(trips.get(4).getStartStation().getLongitude()), 0.00001);
        assertNull(trips.get(4).getEndStation());
    }
}
