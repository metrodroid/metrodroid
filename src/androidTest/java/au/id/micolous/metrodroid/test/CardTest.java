/*
 * CardTest.java
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

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector;
import com.codebutler.farebot.card.ultralight.UltralightCard;
import com.codebutler.farebot.card.ultralight.UltralightPage;
import com.codebutler.farebot.card.ultralight.UnauthorizedUltralightPage;
import com.codebutler.farebot.transit.unknown.UnauthorizedClassicTransitData;
import com.codebutler.farebot.transit.unknown.UnauthorizedUltralightTransitData;
import com.codebutler.farebot.util.Utils;
import com.codebutler.farebot.xml.Base64String;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.simpleframework.xml.Serializer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import au.id.micolous.metrodroid.MetrodroidApplication;

/**
 * Generic card tests
 */

public class CardTest extends TestCase {
    public void testXmlSerialiser() {
        Serializer s = MetrodroidApplication.getInstance().getSerializer();
        Calendar d = new GregorianCalendar(2010, 1, 1, 0, 0, 0);
        d.setTimeZone(TimeZone.getTimeZone("GMT"));

        Card c1 = new ClassicCard(new byte[] {0x00, 0x12, 0x34, 0x56},
                d,
                new ClassicSector[] {});

        String xml = c1.toXml(s);

        assertTrue(xml.contains("scanned_at=\"1264982400000\""));
        assertTrue(xml.contains("id=\"00123456\""));

        Card c2 = Card.fromXml(s, xml);

        assertEquals(d.getTimeInMillis(), c1.getScannedAt().getTimeInMillis());
        assertEquals(c1.getScannedAt().getTimeInMillis(), c2.getScannedAt().getTimeInMillis());

    }

    public void testUnauthorizedUltralight() {
        Calendar d = new GregorianCalendar(2010, 1, 1, 0, 0, 0);
        d.setTimeZone(TimeZone.getTimeZone("GMT"));

        Card c1 = new UltralightCard(Utils.hexStringToByteArray("00123456789abcde"),
                d,
                "MF0ICU2",
                new UltralightPage[] {
                        new UltralightPage(0, Utils.hexStringToByteArray("00123456")),
                        new UltralightPage(1, Utils.hexStringToByteArray("789abcde")),
                        new UltralightPage(2, Utils.hexStringToByteArray("ff000000")),
                        new UltralightPage(3, Utils.hexStringToByteArray("ffffffff")),
                        new UnauthorizedUltralightPage(4), // User memory starts here
                        new UnauthorizedUltralightPage(5),
                        new UnauthorizedUltralightPage(6),
                        new UnauthorizedUltralightPage(7),
                        new UnauthorizedUltralightPage(8),
                        new UnauthorizedUltralightPage(9),
                        new UnauthorizedUltralightPage(10),
                        new UnauthorizedUltralightPage(11),
                        new UnauthorizedUltralightPage(12),
                        new UnauthorizedUltralightPage(13),
                        new UnauthorizedUltralightPage(14),
                        new UnauthorizedUltralightPage(15),
                        new UnauthorizedUltralightPage(16),
                        new UnauthorizedUltralightPage(17),
                        new UnauthorizedUltralightPage(18),
                        new UnauthorizedUltralightPage(19),
                        new UnauthorizedUltralightPage(20),
                        new UnauthorizedUltralightPage(21),
                        new UnauthorizedUltralightPage(22),
                        new UnauthorizedUltralightPage(23),
                        new UnauthorizedUltralightPage(24),
                        new UnauthorizedUltralightPage(25),
                        new UnauthorizedUltralightPage(26),
                        new UnauthorizedUltralightPage(27),
                        new UnauthorizedUltralightPage(28),
                        new UnauthorizedUltralightPage(29),
                        new UnauthorizedUltralightPage(30),
                        new UnauthorizedUltralightPage(31),
                        new UnauthorizedUltralightPage(32),
                        new UnauthorizedUltralightPage(33),
                        new UnauthorizedUltralightPage(34),
                        new UnauthorizedUltralightPage(35),
                        new UnauthorizedUltralightPage(36),
                        new UnauthorizedUltralightPage(37),
                        new UnauthorizedUltralightPage(38),
                        new UnauthorizedUltralightPage(39),
                        new UnauthorizedUltralightPage(40),
                        new UnauthorizedUltralightPage(41),
                        new UnauthorizedUltralightPage(42),
                        new UnauthorizedUltralightPage(43)
                });

        assertTrue(c1.parseTransitData() instanceof UnauthorizedUltralightTransitData);
    }

    public void testUnauthorizedClassic() {
        Calendar d = new GregorianCalendar(2010, 1, 1, 0, 0, 0);
        d.setTimeZone(TimeZone.getTimeZone("GMT"));

        ArrayList<ClassicSector> l = new ArrayList<>();
        for (int x=0; x < 16; x++) {
            l.add(new UnauthorizedClassicSector(x));
        }

        Card c1 = new ClassicCard(Utils.hexStringToByteArray("12345678"),
                d,
                l.toArray(new ClassicSector[l.size()]));

        assertTrue(c1.parseTransitData() instanceof UnauthorizedClassicTransitData);
    }
}
