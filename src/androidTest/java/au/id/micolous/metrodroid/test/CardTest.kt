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
package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.classic.ClassicBlock
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile
import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightPage
import au.id.micolous.metrodroid.card.ultralight.UnauthorizedUltralightPage
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.transit.unknown.BlankClassicTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedDesfireTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedUltralightTransitData
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Generic card tests
 */
class CardTest {
    @Test
    fun testXmlSerialiser() {
        val d = GregorianCalendar(2010, 1, 1, 0, 0, 0)
        d.timeZone = TimeZone.getTimeZone("GMT")

        val c1 = ClassicCard(ImmutableByteArray.fromHex("00123456"),
                d,
                emptyList())

        val xml = c1.toXml()

        assertTrue(xml.contains("scanned_at=\"1264982400000\""))
        assertTrue(xml.contains("id=\"00123456\""))

        val c2 = Card.fromXml(xml)

        assertEquals(d.timeInMillis, c1.scannedAt.timeInMillis)
        assertEquals(c1.scannedAt.timeInMillis, c2.scannedAt.timeInMillis)

    }

    @Test
    fun testUnauthorizedUltralight() {
        val d = GregorianCalendar(2010, 1, 1, 0, 0, 0)
        d.timeZone = TimeZone.getTimeZone("GMT")

        val c1 = UltralightCard(ImmutableByteArray.fromHex("00123456789abcde"),
                d,
                "MF0ICU2",
                Arrays.asList(
                        UltralightPage(0, ImmutableByteArray.fromHex("00123456")),
                        UltralightPage(1, ImmutableByteArray.fromHex("789abcde")),
                        UltralightPage(2, ImmutableByteArray.fromHex("ff000000")),
                        UltralightPage(3, ImmutableByteArray.fromHex("ffffffff")),
                        UnauthorizedUltralightPage(4), // User memory starts here
                        UnauthorizedUltralightPage(5),
                        UnauthorizedUltralightPage(6),
                        UnauthorizedUltralightPage(7),
                        UnauthorizedUltralightPage(8),
                        UnauthorizedUltralightPage(9),
                        UnauthorizedUltralightPage(10),
                        UnauthorizedUltralightPage(11),
                        UnauthorizedUltralightPage(12),
                        UnauthorizedUltralightPage(13),
                        UnauthorizedUltralightPage(14),
                        UnauthorizedUltralightPage(15),
                        UnauthorizedUltralightPage(16),
                        UnauthorizedUltralightPage(17),
                        UnauthorizedUltralightPage(18),
                        UnauthorizedUltralightPage(19),
                        UnauthorizedUltralightPage(20),
                        UnauthorizedUltralightPage(21),
                        UnauthorizedUltralightPage(22),
                        UnauthorizedUltralightPage(23),
                        UnauthorizedUltralightPage(24),
                        UnauthorizedUltralightPage(25),
                        UnauthorizedUltralightPage(26),
                        UnauthorizedUltralightPage(27),
                        UnauthorizedUltralightPage(28),
                        UnauthorizedUltralightPage(29),
                        UnauthorizedUltralightPage(30),
                        UnauthorizedUltralightPage(31),
                        UnauthorizedUltralightPage(32),
                        UnauthorizedUltralightPage(33),
                        UnauthorizedUltralightPage(34),
                        UnauthorizedUltralightPage(35),
                        UnauthorizedUltralightPage(36),
                        UnauthorizedUltralightPage(37),
                        UnauthorizedUltralightPage(38),
                        UnauthorizedUltralightPage(39),
                        UnauthorizedUltralightPage(40),
                        UnauthorizedUltralightPage(41),
                        UnauthorizedUltralightPage(42),
                        UnauthorizedUltralightPage(43)
                ))

        assertTrue(c1.parseTransitData() is UnauthorizedUltralightTransitData)
    }

    @Test
    fun testUnauthorizedClassic() {
        val e = byteArrayOf(0x6d.toByte(), 0x65.toByte(), 0x74.toByte(), 0x72.toByte(), 0x6f.toByte(), 0x64.toByte(), 0x72.toByte(), 0x6f.toByte(), 0x69.toByte(), 0x64.toByte(), 0x43.toByte(), 0x6c.toByte(), 0x61.toByte(), 0x73.toByte(), 0x73.toByte(), 0x69.toByte())
        val k = ClassicSectorKey.fromDump(ImmutableByteArray(6),
                ClassicSectorKey.KeyType.A, "test")
        val d = GregorianCalendar(2010, 1, 1, 0, 0, 0)
        d.timeZone = TimeZone.getTimeZone("GMT")

        var l = arrayOfNulls<ClassicSector>(16)
        for (x in 0..15) {
            l[x] = UnauthorizedClassicSector(x)
        }

        val c1 = ClassicCard(ImmutableByteArray.fromHex("12345678"), d, Arrays.asList<ClassicSector>(*l))

        assertTrue(c1.parseTransitData() is UnauthorizedClassicTransitData)

        // Build a card with partly readable data.
        val b = arrayOfNulls<ClassicBlock>(4)
        for (y in 0..3) {
            b[y] = ClassicBlock.create(ClassicBlock.TYPE_DATA, y, ImmutableByteArray.fromByteArray(e))
        }

        l[2] = ClassicSector(2, b, k)
        val c2 = ClassicCard(ImmutableByteArray.fromHex("12345678"), d, Arrays.asList<ClassicSector>(*l))

        assertFalse(c2.parseTransitData() is UnauthorizedClassicTransitData)

        // Build a card with all readable data.
        l = arrayOfNulls(16)
        for (x in 0..15) {
            l[x] = ClassicSector(x, b, k)
        }

        val c3 = ClassicCard(ImmutableByteArray.fromHex("12345678"), d, Arrays.asList<ClassicSector>(*l))

        assertFalse(c3.parseTransitData() is UnauthorizedClassicTransitData)
    }

    @Test
    fun testBlankMifareClassic() {
        val all00Bytes = ImmutableByteArray(16)
        val allFFBytes = ImmutableByteArray(16) { i -> 0xff.toByte() }
        val otherBytes = ImmutableByteArray(16) { i -> (i + 1).toByte() }

        val k = ClassicSectorKey.fromDump(ImmutableByteArray(6),
                ClassicSectorKey.KeyType.A, "test")
        val d = GregorianCalendar(2010, 1, 1, 0, 0, 0)
        d.timeZone = TimeZone.getTimeZone("GMT")

        val all00Blocks = arrayOfNulls<ClassicBlock>(4)
        val allFFBlocks = arrayOfNulls<ClassicBlock>(4)
        val otherBlocks = arrayOfNulls<ClassicBlock>(4)

        for (x in 0..3) {
            all00Blocks[x] = ClassicBlock.create(ClassicBlock.TYPE_DATA, x, all00Bytes)
            allFFBlocks[x] = ClassicBlock.create(ClassicBlock.TYPE_DATA, x, allFFBytes)
            otherBlocks[x] = ClassicBlock.create(ClassicBlock.TYPE_DATA, x, otherBytes)
        }

        val all00Sectors = arrayOfNulls<ClassicSector>(16)
        val allFFSectors = arrayOfNulls<ClassicSector>(16)
        val otherSectors = arrayOfNulls<ClassicSector>(16)

        for (x in 0..15) {
            all00Sectors[x] = ClassicSector(x, all00Blocks, k)
            allFFSectors[x] = ClassicSector(x, allFFBlocks, k)
            otherSectors[x] = ClassicSector(x, otherBlocks, k)
        }

        val all00Card = ClassicCard(ImmutableByteArray.fromHex("12345678"), d, Arrays.asList<ClassicSector>(*all00Sectors))
        val allFFCard = ClassicCard(ImmutableByteArray.fromHex("87654321"), d, Arrays.asList<ClassicSector>(*allFFSectors))
        val otherCard = ClassicCard(ImmutableByteArray.fromHex("21436587"), d, Arrays.asList<ClassicSector>(*otherSectors))

        assertTrue(
                message = "A card with all 00 in its blocks is BlankClassicTransitData",
                actual = all00Card.parseTransitData() is BlankClassicTransitData)
        assertTrue(
                message = "A card with all FF in its blocks is BlankClassicTransitData",
                actual = allFFCard.parseTransitData() is BlankClassicTransitData)

        // If the tests crash here, this is a bug in the other reader module. We shouldn't be able
        // to crash reader modules with garbage data.
        assertFalse(
                message = "A card with other data in its blocks is not BlankClassicTransitData",
                actual = otherCard.parseTransitData() is BlankClassicTransitData)
    }

    @Test
    fun testUnauthorizedDesfire() {
        val d = GregorianCalendar(2010, 1, 1, 0, 0, 0)
        d.timeZone = TimeZone.getTimeZone("GMT")

        // Card with no files at all.
        val c1 = DesfireCard(ImmutableByteArray.fromHex("47504C7633"),
                d, null, emptyList())

        assertTrue(c1.parseTransitData() is UnauthorizedDesfireTransitData)

        // Card with only locked files.
        val c2 = DesfireCard(ImmutableByteArray.fromHex("6D6574726F"),
                d, null, listOf(DesfireApplication(0x6472, listOf<DesfireFile>(UnauthorizedDesfireFile(0x6f69, "Authentication error: 64", null))))
        )

        assertTrue(c2.parseTransitData() is UnauthorizedDesfireTransitData)

        // Card with unlocked file.
        val c3 = DesfireCard(ImmutableByteArray.fromHex("6D6574726F"),
                d, null, listOf(DesfireApplication(0x6472, listOf(DesfireFile.create(0x6f69,
                DesfireFileSettings.create(ImmutableByteArray(16)),
                byteArrayOf(0x6d.toByte(), 0x69.toByte(), 0x63.toByte(), 0x6f.toByte(), 0x6c.toByte(), 0x6f.toByte(), 0x75.toByte(), 0x73.toByte()))))))

        assertFalse(c3.parseTransitData() is UnauthorizedDesfireTransitData)
    }
}
