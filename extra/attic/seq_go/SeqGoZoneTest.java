package au.id.micolous.farebot.test;

import com.codebutler.farebot.transit.seq_go.SeqGoZoneCalculator;

import junit.framework.TestCase;

/**
 * Unit tests for SeqGoZoneCalculator
 */
public class SeqGoZoneTest extends TestCase {
    public void testEqualZones() {
        String[] zones = SeqGoZoneCalculator.zonesTravelled("2", "2", false);
        assertEquals(1, zones.length);
        assertEquals("2", zones[0]);

        zones = SeqGoZoneCalculator.zonesTravelled("2", "2/3", false);
        assertEquals(1, zones.length);
        assertEquals("2", zones[0]);

        zones = SeqGoZoneCalculator.zonesTravelled("1/2", "2/3", false);
        assertEquals(1, zones.length);
        assertEquals("2", zones[0]);
    }

    public void testReversedZones() {
        String[] zones = SeqGoZoneCalculator.zonesTravelled("2/1", "3/2", false);
        assertEquals(1, zones.length);
        assertEquals("2", zones[0]);
    }

    public void testStandardZones() {
        String[] zones = SeqGoZoneCalculator.zonesTravelled("1", "2", false);
        assertEquals(2, zones.length);
        assertEquals("1", zones[0]);
        assertEquals("2", zones[1]);

        zones = SeqGoZoneCalculator.zonesTravelled("1", "10", false);
        assertEquals(10, zones.length);
        assertEquals("1", zones[0]);
        assertEquals("2", zones[1]);
        assertEquals("3", zones[2]);
        assertEquals("4", zones[3]);
        assertEquals("5", zones[4]);
        assertEquals("6", zones[5]);
        assertEquals("7", zones[6]);
        assertEquals("8", zones[7]);
        assertEquals("9", zones[8]);
        assertEquals("10", zones[9]);
    }

    public void testAirtrainZones() {
        String[] zones = SeqGoZoneCalculator.zonesTravelled("1", "airtrain", false);
        assertEquals(2, zones.length);
        assertEquals("airtrain", zones[0]);
        assertEquals("1", zones[1]);

        zones = SeqGoZoneCalculator.zonesTravelled("airtrain", "1", false);
        assertEquals(2, zones.length);
        assertEquals("airtrain", zones[0]);
        assertEquals("1", zones[1]);

        zones = SeqGoZoneCalculator.zonesTravelled("3", "airtrain", false);
        assertEquals(4, zones.length);
        assertEquals("airtrain", zones[0]);
        assertEquals("1", zones[1]);
        assertEquals("2", zones[2]);
        assertEquals("3", zones[3]);

        zones = SeqGoZoneCalculator.zonesTravelled("airtrain", "airtrain", false);
        assertEquals(1, zones.length);
        assertEquals("airtrain_xfer", zones[0]);

        zones = SeqGoZoneCalculator.zonesTravelled("1", "airtrain", true);
        assertEquals(1, zones.length);
        assertEquals("airtrain", zones[0]);

        zones = SeqGoZoneCalculator.zonesTravelled("airtrain", "1", true);
        assertEquals(1, zones.length);
        assertEquals("airtrain", zones[0]);

        zones = SeqGoZoneCalculator.zonesTravelled("2", "airtrain", true);
        assertEquals(1, zones.length);
        assertEquals("airtrain", zones[0]);

        zones = SeqGoZoneCalculator.zonesTravelled("airtrain", "airtrain", true);
        assertEquals(1, zones.length);
        assertEquals("airtrain_xfer", zones[0]);
    }
}
