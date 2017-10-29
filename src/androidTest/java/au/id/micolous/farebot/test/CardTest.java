package au.id.micolous.farebot.test;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;

import junit.framework.TestCase;

import org.simpleframework.xml.Serializer;

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
}
