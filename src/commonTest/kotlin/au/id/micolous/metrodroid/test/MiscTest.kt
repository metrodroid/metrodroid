package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.*
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MiscTest {
    @Test
    fun testPlusAssign() {
        val sb = StringBuilder("ABC")
        sb += "DEF"
        assertEquals("ABCDEF", sb.toString())
    }

    @Test
    fun testBoolean() {
        assertEquals(null,
            false.ifTrue { assertTrue(false, "Shouldn't be reached"); 123 })
        assertEquals(null,
            true.ifFalse { assertTrue(false, "Shouldn't be reached"); 123 })
        assertEquals(123, true.ifTrue { 123 })
        assertEquals(123, false.ifFalse { 123 })
    }

    @Test
    fun testMakeFilename() {
        assertEquals("Metrodroid-abcdef-20200501-093553.json",
            makeFilename("abcdef",
                TimestampFull(MetroTimeZone.HELSINKI, 2020, Month.MAY, 1, 12, 35, 53),
        "json", 0))
        assertEquals("Metrodroid-abcdef-20200501-093553.json",
            makeFilename("abcdef",
                TimestampFull(MetroTimeZone.HELSINKI, 2020, Month.MAY, 1, 12, 35, 53),
                "json"))
        assertEquals("Metrodroid-abcdef-20200501-093553-5.json",
            makeFilename("abcdef",
                TimestampFull(MetroTimeZone.HELSINKI, 2020, Month.MAY, 1, 12, 35, 53),
                "json", 5))
        assertEquals("Metrodroid-abcdef-20200501-093553-5.json",
            makeFilename(
                Card(tagId = ImmutableByteArray.fromHex("abcdef"),
                scannedAt = TimestampFull(MetroTimeZone.HELSINKI, 2020, Month.MAY, 1, 12, 35, 53)),
                5))
        assertEquals("Metrodroid-abcdef-20200501-093553.json",
            makeFilename(
                Card(tagId = ImmutableByteArray.fromHex("abcdef"),
                    scannedAt = TimestampFull(MetroTimeZone.HELSINKI, 2020, Month.MAY, 1, 12, 35, 53))))
    }
}
