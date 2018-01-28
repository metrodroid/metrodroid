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

import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.files.DesfireFile;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.opal.OpalTransitData;
import com.codebutler.farebot.util.Utils;

import junit.framework.TestCase;

import java.util.Calendar;

/**
 * Tests for Opal card
 */

public class OpalTest extends TestCase {
    public void testDemoCard() {
        // This is mocked-up data, probably has a wrong checksum.
        byte[] demoData = Utils.hexStringToByteArray("87d61200e004002a0014cc44a4133930");

        // Construct a card to hold the data.
        DesfireFile f = DesfireFile.create(OpalTransitData.FILE_ID, null, demoData);
        DesfireApplication a = new DesfireApplication(OpalTransitData.APP_ID, new DesfireFile[] { f });
        DesfireCard c = new DesfireCard(new byte[] {0, 1, 2, 3},
                Calendar.getInstance(),
                null,
                new DesfireApplication[] { a });

        // Test TransitIdentity
        TransitIdentity i = c.parseTransitIdentity();
        assertEquals(OpalTransitData.NAME, i.getName());
        assertEquals("3085220012345670", i.getSerialNumber());

        // Test TransitData
        TransitData d = c.parseTransitData();
        assertTrue("TransitData must be instance of OpalTransitData", d instanceof OpalTransitData);

        OpalTransitData o = (OpalTransitData)d;
        assertEquals("3085220012345670", o.getSerialNumber());
        assertEquals(336, o.getBalance().intValue());
        assertEquals(0, o.getSubscriptions().length);
    }

}
