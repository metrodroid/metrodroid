/*
 * MykiTest.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Calendar;

import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.serialonly.MykiTransitData;
import au.id.micolous.metrodroid.util.Utils;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(JUnit4.class)
public class MykiTest  {
    private DesfireCard constructMykiCardFromHexString(String s) {
        byte[] demoData = Utils.hexStringToByteArray(s);

        // Construct a card to hold the data.
        DesfireFile f = DesfireFile.create(15, null, demoData);
        DesfireApplication a = new DesfireApplication(MykiTransitData.APP_ID_1, new DesfireFile[] { f });
        DesfireApplication a2 = new DesfireApplication(MykiTransitData.APP_ID_2, new DesfireFile[] {});
        return new DesfireCard(new byte[] {0, 1, 2, 3},
                Calendar.getInstance(),
                null,
                new DesfireApplication[] { a, a2 });
    }

    @Test
    public void testDemoCard() {
        // This is mocked-up, incomplete data.
        DesfireCard c = constructMykiCardFromHexString("C9B404004E61BC000000000000000000");

        // Test TransitIdentity
        TransitIdentity i = c.parseTransitIdentity();
        assertEquals(MykiTransitData.NAME, i.getName());
        assertEquals("308425123456780", i.getSerialNumber());

        // Test TransitData
        TransitData d = c.parseTransitData();
        assertTrue("TransitData must be instance of MykiTransitData", d instanceof MykiTransitData);

        MykiTransitData o = (MykiTransitData)d;
        assertEquals("308425123456780", o.getSerialNumber());
    }
}
