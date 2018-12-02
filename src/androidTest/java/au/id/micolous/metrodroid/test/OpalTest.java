/*
 * OpalTest.java
 *
 * Copyright 2017-2018 Michael Farrell <micolous+git@gmail.com>
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

import android.os.Build;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.opal.OpalData;
import au.id.micolous.metrodroid.transit.opal.OpalTransitData;
import au.id.micolous.metrodroid.util.Utils;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests for Opal card
 */
@RunWith(JUnit4.class)
public class OpalTest {
    private TimeZone originalTz;

    @Before
    public void setUp() {
        originalTz = TimeZone.getDefault();
        TimeZone.setDefault(OpalTransitData.TIME_ZONE);
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTz);
    }

    private DesfireCard constructOpalCardFromHexString(String s) {
        byte[] demoData = Utils.hexStringToByteArray(s);

        // Construct a card to hold the data.
        DesfireFile f = DesfireFile.create(OpalTransitData.FILE_ID, null, demoData);
        DesfireApplication a = new DesfireApplication(OpalTransitData.APP_ID, new DesfireFile[] { f });
        return new DesfireCard(new byte[] {0, 1, 2, 3},
                Calendar.getInstance(),
                null,
                new DesfireApplication[] { a });
    }

    @Test
    public void testDemoCard() {
        // This is mocked-up data, probably has a wrong checksum.
        DesfireCard c = constructOpalCardFromHexString("87d61200e004002a0014cc44a4133930");

        // Test TransitIdentity
        TransitIdentity i = c.parseTransitIdentity();
        assertEquals(OpalTransitData.NAME, i.getName());
        assertEquals("3085220012345670", i.getSerialNumber());

        // Test TransitData
        TransitData d = c.parseTransitData();
        assertTrue("TransitData must be instance of OpalTransitData", d instanceof OpalTransitData);

        OpalTransitData o = (OpalTransitData)d;
        assertEquals("3085220012345670", o.getSerialNumber());
        assertEquals(TransitCurrency.AUD(336), o.getBalance());
        assertEquals(0, o.getSubscriptions().size());
        // 2015-10-05 09:06 UTC+11
        assertEquals("2015-10-04 22:06", Utils.isoDateTimeFormat(o.getLastTransactionTime()));
        assertEquals(OpalData.MODE_BUS, o.getLastTransactionMode());
        assertEquals(OpalData.ACTION_JOURNEY_COMPLETED_DISTANCE, o.getLastTransaction());
        assertEquals(39, o.getLastTransactionNumber());
        assertEquals(1, o.getWeeklyTrips());
    }

    @Test
    public void testDaylightSavings() {
        // This is all mocked-up data, probably has a wrong checksum.

        // 2018-03-31 09:00 UTC+11
        // 2018-03-30 22:00 UTC
        DesfireCard c = constructOpalCardFromHexString("85D25E07230520A70044DA380419FFFF");

        OpalTransitData o = (OpalTransitData)c.parseTransitData();
        assertEquals("2018-03-30 22:00", Utils.isoDateTimeFormat(o.getLastTransactionTime()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals("2018-03-30T22:00Z", o.getLastTransactionTime().toInstant().atOffset(ZoneOffset.UTC).toString());
        }

        // DST transition is at 2018-04-01 03:00

        // 2018-04-01 09:00 UTC+10
        // 2018-03-31 23:00 UTC
        c = constructOpalCardFromHexString("85D25E07430520A70048DA380419FFFF");

        o = (OpalTransitData)c.parseTransitData();
        assertEquals("2018-03-31 23:00", Utils.isoDateTimeFormat(o.getLastTransactionTime()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals("2018-03-31T23:00Z", o.getLastTransactionTime().toInstant().atOffset(ZoneOffset.UTC).toString());
        }
    }

}
