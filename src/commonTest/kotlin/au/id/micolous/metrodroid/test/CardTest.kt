/*
 * CardTest.kt
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
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.card.classic.*
import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.files.RawDesfireFile
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightPage
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.unknown.BlankClassicTransitData
import au.id.micolous.metrodroid.transit.unknown.BlankDesfireTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedDesfireTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedUltralightTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Generic card tests
 */
class CardTest : BaseInstrumentedTest() {
    @Test
    fun testJsonSerialiser() {
        val d = TimestampFull(MetroTimeZone.UTC, 2010, 1, 1, 0, 0, 0)

        val c1 = Card(ImmutableByteArray.fromHex("00123456"),
                d,
                mifareClassic = ClassicCard(emptyList<ClassicSectorRaw>()))

        val json = JsonKotlinFormat.writeCard(c1)

        assertTrue(json.contains("\"timeInMillis\": 1264982400000"))
        assertTrue(json.contains("\"tagId\": \"00123456\""))
        Log.d("CardTest", "JSON serialized to $json")

        val c2 = JsonKotlinFormat.readCard(json)

        assertEquals(d.timeInMillis, c1.scannedAt.timeInMillis)
        assertEquals(c1.scannedAt.timeInMillis, c2.scannedAt.timeInMillis)
    }

    @Test
    fun testUnauthorizedUltralight() {
        val d = TimestampFull(MetroTimeZone.UTC, 2010, 1, 1, 0, 0, 0)

        val u1 = UltralightCard(
                "MF0ICU2",
                listOf(
                        UltralightPage(ImmutableByteArray.fromHex("00123456")),
                        UltralightPage(ImmutableByteArray.fromHex("789abcde")),
                        UltralightPage(ImmutableByteArray.fromHex("ff000000")),
                        UltralightPage(ImmutableByteArray.fromHex("ffffffff")),
                        UltralightPage.unauthorized(), // User memory starts here
                        UltralightPage.unauthorized(), // page(5),
                        UltralightPage.unauthorized(), // page(6),
                        UltralightPage.unauthorized(), // page(7),
                        UltralightPage.unauthorized(), // page(8),
                        UltralightPage.unauthorized(), // page(9),
                        UltralightPage.unauthorized(), // page(10),
                        UltralightPage.unauthorized(), // page(11),
                        UltralightPage.unauthorized(), // page(12),
                        UltralightPage.unauthorized(), // page(13),
                        UltralightPage.unauthorized(), // page(14),
                        UltralightPage.unauthorized(), // page(15),
                        UltralightPage.unauthorized(), // page(16),
                        UltralightPage.unauthorized(), // page(17),
                        UltralightPage.unauthorized(), // page(18),
                        UltralightPage.unauthorized(), // page(19),
                        UltralightPage.unauthorized(), // page(20),
                        UltralightPage.unauthorized(), // page(21),
                        UltralightPage.unauthorized(), // page(22),
                        UltralightPage.unauthorized(), // page(23),
                        UltralightPage.unauthorized(), // page(24),
                        UltralightPage.unauthorized(), // page(25),
                        UltralightPage.unauthorized(), // page(26),
                        UltralightPage.unauthorized(), // page(27),
                        UltralightPage.unauthorized(), // page(28),
                        UltralightPage.unauthorized(), // page(29),
                        UltralightPage.unauthorized(), // page(30),
                        UltralightPage.unauthorized(), // page(31),
                        UltralightPage.unauthorized(), // page(32),
                        UltralightPage.unauthorized(), // page(33),
                        UltralightPage.unauthorized(), // page(34),
                        UltralightPage.unauthorized(), // page(35),
                        UltralightPage.unauthorized(), // page(36),
                        UltralightPage.unauthorized(), // page(37),
                        UltralightPage.unauthorized(), // page(38),
                        UltralightPage.unauthorized(), // page(39),
                        UltralightPage.unauthorized(), // page(40),
                        UltralightPage.unauthorized(), // page(41),
                        UltralightPage.unauthorized(), // page(42),
                        UltralightPage.unauthorized() // page(43)
                ))
        val c1 = Card(
                ImmutableByteArray.fromHex("00123456789abcde"),
                d, mifareUltralight = u1)

        assertTrue(c1.parseTransitData() is UnauthorizedUltralightTransitData)
    }

    @Test
    fun testUnauthorizedClassic() {
        val e = ImmutableByteArray.ofB(0x6d, 0x65, 0x74, 0x72, 0x6f, 0x64, 0x72, 0x6f, 0x69, 0x64, 0x43, 0x6c, 0x61, 0x73, 0x73, 0x69)
        val k = ImmutableByteArray(6)
        val d = TimestampFull(MetroTimeZone.UTC, 2010, 1, 1, 0, 0, 0)

        val l = (0..15).map { UnauthorizedClassicSector() }.toMutableList<ClassicSector>()

        val c1 = ClassicCard(l)

        assertTrue(c1.parseTransitData() is UnauthorizedClassicTransitData)

        // Build a card with partly readable data.
        val b = (0..3).map { e }

        l[2] = ClassicSector.create(ClassicSectorRaw(blocks = b, keyA = k, keyB = null))
        val c2 = ClassicCard(l)

        assertFalse(c2.parseTransitData() is UnauthorizedClassicTransitData)

        // Build a card with all readable data.
        for (x in 0..15) {
            l[x] = ClassicSector.create(ClassicSectorRaw(b, keyA = k, keyB = null))
        }

        val c3 = ClassicCard(l)

        assertFalse(c3.parseTransitData() is UnauthorizedClassicTransitData)
    }

    @Test
    fun testBlankMifareClassic() {
        val all00Bytes = ImmutableByteArray(16)
        val allFFBytes = ImmutableByteArray(16) { 0xff.toByte() }
        val otherBytes = ImmutableByteArray(16) { i -> (i + 1).toByte() }

        val k = ImmutableByteArray(6)
        val d = TimestampFull(MetroTimeZone.UTC, 2010, 1, 1, 0, 0, 0)

        val all00Blocks = List(4) { all00Bytes }
        val allFFBlocks = List(4) { allFFBytes }
        val otherBlocks = List(4) { otherBytes }

        val all00Sectors = (0..15).map { ClassicSectorValid(ClassicSectorRaw(blocks = all00Blocks, keyA = k, keyB = null)) }
        val allFFSectors = (0..15).map { ClassicSectorValid(ClassicSectorRaw(blocks = allFFBlocks, keyA = k, keyB = null)) }
        val otherSectors = (0..15).map { ClassicSectorValid(ClassicSectorRaw(blocks = otherBlocks, keyA = k, keyB = null)) }

        val all00Card = ClassicCard(all00Sectors)
        val allFFCard = ClassicCard(allFFSectors)
        val otherCard = ClassicCard(otherSectors)

        assertTrue(all00Card.parseTransitData() is BlankClassicTransitData,
                "A card with all 00 in its blocks is BlankClassicTransitData")
        assertTrue(allFFCard.parseTransitData() is BlankClassicTransitData,
                "A card with all FF in its blocks is BlankClassicTransitData")

        // If the tests crash here, this is a bug in the other reader module. We shouldn't be able
        // to crash reader modules with garbage data.
        assertFalse(otherCard.parseTransitData() is BlankClassicTransitData,
                "A card with other data in its blocks is not BlankClassicTransitData")
    }

    @Test
    fun testUnauthorizedDesfire() {
        val d = TimestampFull(MetroTimeZone.UTC, 2010, 1, 1, 0, 0, 0)

        // Card with no files at all.
        val c1 = DesfireCard(ImmutableByteArray.empty(), emptyMap())

        assertTrue(c1.parseTransitData() is BlankDesfireTransitData)

        // Card with only locked files.
        val c2 = DesfireCard(ImmutableByteArray.empty(),
                mapOf(0x6472 to DesfireApplication(
                        files = mapOf(0x6f69 to RawDesfireFile(
                                settings = ImmutableByteArray.fromHex("00000000000000"),
                                data = null,
                                error = "Authentication error: 64",
                                isUnauthorized = true)), authLog = emptyList()))
        )

        assertTrue(c2.parseTransitData() is UnauthorizedDesfireTransitData)

        // Card with unlocked file.
        val c3 = DesfireCard(ImmutableByteArray.empty(),
                mapOf(0x6472 to DesfireApplication(
                        files = mapOf(0x6f69 to RawDesfireFile(settings = ImmutableByteArray.fromHex("00000000000000"),
                                data = ImmutableByteArray.fromHex("6d69636f6c6f7573"))),
                        authLog = emptyList())))

        assertFalse(c3.parseTransitData() is UnauthorizedDesfireTransitData)
    }
}
