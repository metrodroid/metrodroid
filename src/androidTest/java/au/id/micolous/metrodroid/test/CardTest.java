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

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightPage;
import au.id.micolous.metrodroid.card.ultralight.UnauthorizedUltralightPage;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.transit.unknown.BlankClassicTransitData;
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData;
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedDesfireTransitData;
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedUltralightTransitData;
import au.id.micolous.metrodroid.util.Utils;

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
        byte[] e = new byte[] { (byte) 0x6d, (byte) 0x65, (byte) 0x74, (byte) 0x72, (byte) 0x6f,
                                (byte) 0x64, (byte) 0x72, (byte) 0x6f, (byte) 0x69, (byte) 0x64,
                                (byte) 0x43, (byte) 0x6c, (byte) 0x61, (byte) 0x73, (byte) 0x73,
                                (byte) 0x69 };
        byte[] k = new byte[] { 0, 0, 0, 0, 0, 0 };
        Calendar d = new GregorianCalendar(2010, 1, 1, 0, 0, 0);
        d.setTimeZone(TimeZone.getTimeZone("GMT"));

        ClassicSector[] l = new ClassicSector[16];
        for (int x=0; x < 16; x++) {
            l[x] = new UnauthorizedClassicSector(x);
        }

        Card c1 = new ClassicCard(Utils.hexStringToByteArray("12345678"), d, l);

        assertTrue(c1.parseTransitData() instanceof UnauthorizedClassicTransitData);

        // Build a card with partly readable data.
        ClassicBlock[] b = new ClassicBlock[4];
        for (int y=0; y < 4; y++) {
            b[y] = ClassicBlock.create(ClassicBlock.TYPE_DATA, y, e);
        }

        l[2] = new ClassicSector(2, b, k, ClassicSectorKey.TYPE_KEYA);
        Card c2 = new ClassicCard(Utils.hexStringToByteArray("12345678"), d, l);

        assertFalse(c2.parseTransitData() instanceof UnauthorizedClassicTransitData);

        // Build a card with all readable data.
        l = new ClassicSector[16];
        for (int x=0; x < 16; x++) {
            l[x] = new ClassicSector(x, b, k, ClassicSectorKey.TYPE_KEYA);
        }

        Card c3 = new ClassicCard(Utils.hexStringToByteArray("12345678"), d, l);

        assertFalse(c3.parseTransitData() instanceof UnauthorizedClassicTransitData);
    }

    public void testBlankMifareClassic() {
        byte[] all00Bytes = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        byte[] allFFBytes = new byte[] {
                (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                (byte)0xff, (byte)0xff };
        byte[] otherBytes = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

        byte[] k = new byte[] { 0, 0, 0, 0, 0, 0 };
        Calendar d = new GregorianCalendar(2010, 1, 1, 0, 0, 0);
        d.setTimeZone(TimeZone.getTimeZone("GMT"));

        ClassicBlock[] all00Blocks = new ClassicBlock[4];
        ClassicBlock[] allFFBlocks = new ClassicBlock[4];
        ClassicBlock[] otherBlocks = new ClassicBlock[4];

        for (int x=0; x < 4; x++) {
            all00Blocks[x] = ClassicBlock.create(ClassicBlock.TYPE_DATA, x, all00Bytes);
            allFFBlocks[x] = ClassicBlock.create(ClassicBlock.TYPE_DATA, x, allFFBytes);
            otherBlocks[x] = ClassicBlock.create(ClassicBlock.TYPE_DATA, x, otherBytes);
        }

        ClassicSector[] all00Sectors = new ClassicSector[16];
        ClassicSector[] allFFSectors = new ClassicSector[16];
        ClassicSector[] otherSectors = new ClassicSector[16];

        for (int x=0; x < 16; x++) {
            all00Sectors[x] = new ClassicSector(x, all00Blocks, k, ClassicSectorKey.TYPE_KEYA);
            allFFSectors[x] = new ClassicSector(x, allFFBlocks, k, ClassicSectorKey.TYPE_KEYA);
            otherSectors[x] = new ClassicSector(x, otherBlocks, k, ClassicSectorKey.TYPE_KEYA);
        }

        Card all00Card = new ClassicCard(Utils.hexStringToByteArray("12345678"), d, all00Sectors);
        Card allFFCard = new ClassicCard(Utils.hexStringToByteArray("87654321"), d, allFFSectors);
        Card otherCard = new ClassicCard(Utils.hexStringToByteArray("21436587"), d, otherSectors);

        assertTrue("A card with all 00 in its blocks is BlankClassicTransitData",
                all00Card.parseTransitData() instanceof BlankClassicTransitData);
        assertTrue("A card with all FF in its blocks is BlankClassicTransitData",
                allFFCard.parseTransitData() instanceof BlankClassicTransitData);

        // If the tests crash here, this is a bug in the other reader module. We shouldn't be able
        // to crash reader modules with garbage data.
        assertFalse("A card with other data in its blocks is not BlankClassicTransitData",
                otherCard.parseTransitData() instanceof BlankClassicTransitData);
    }

    public void testUnauthorizedDesfire() {
        Calendar d = new GregorianCalendar(2010, 1, 1, 0, 0, 0);
        d.setTimeZone(TimeZone.getTimeZone("GMT"));

        // Card with no files at all.
        Card c1 = new DesfireCard(Utils.hexStringToByteArray("47504C7633"),
                d, null, new DesfireApplication[] {});

        assertTrue(c1.parseTransitData() instanceof UnauthorizedDesfireTransitData);

        // Card with only locked files.
        Card c2 = new DesfireCard(Utils.hexStringToByteArray("6D6574726F"),
                d, null, new DesfireApplication[] {
                new DesfireApplication(0x6472, new DesfireFile[] {
                  new UnauthorizedDesfireFile(0x6f69, "Authentication error: 64", null)
                })
        });

        assertTrue(c2.parseTransitData() instanceof UnauthorizedDesfireTransitData);

        // Card with unlocked file.
        Card c3 = new DesfireCard(Utils.hexStringToByteArray("6D6574726F"),
                d, null, new DesfireApplication[] {
                new DesfireApplication(0x6472, new DesfireFile[] {
                        DesfireFile.create(0x6f69, null,
                                new byte[] { (byte) 0x6d, (byte) 0x69, (byte) 0x63, (byte) 0x6f,
                                             (byte) 0x6c, (byte) 0x6f, (byte) 0x75, (byte) 0x73 })
                })
        });

        assertFalse(c3.parseTransitData() instanceof UnauthorizedDesfireTransitData);
    }
}
