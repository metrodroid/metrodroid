package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.calypso.CalypsoData
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences
import kotlin.test.*

class NumberTest : BaseInstrumentedTest() {
    @Test
    fun testBCD() {
        assertTrue(NumberUtils.isValidBCD(0x123456))
        assertFalse(NumberUtils.isValidBCD(0x1234a6))
        assertEquals(0x123456, NumberUtils.intToBCD(123456))
    }

    @Test
    fun testDigitSum() {
        assertEquals(60, NumberUtils.getDigitSum(12345678912345))
    }

    @Test
    fun testLog10() {
        assertEquals(0, NumberUtils.log10floor(9))
        assertEquals(1, NumberUtils.log10floor(10))
        assertEquals(1, NumberUtils.log10floor(99))
        assertEquals(2, NumberUtils.log10floor(100))
        assertEquals(6, NumberUtils.log10floor(1234567))
    }

    @Test
    fun testDigits() {
        assertContentEquals(intArrayOf(1, 2, 3, 4, 5, 6, 7),
            NumberUtils.digitsOf(1234567))
    }

    @Test
    fun testLookupLocalize() {
        setLocale("en-US")
        Preferences.showRawStationIds = false
        assertEquals("Ascom", CalypsoData.getCompanyName(0x3))
        assertEquals("Unknown (0x77)", CalypsoData.getCompanyName(0x77))
        Preferences.showRawStationIds = true
        assertEquals("Ascom [0x3]", CalypsoData.getCompanyName(0x3))
        assertEquals("Unknown (0x77)", CalypsoData.getCompanyName(0x77))
    }

    @Test
    fun testLookup() {
        val mp = mapOf(
            1 to "Test A",
            2 to "Test B",
            3 to "Test C"
        )
        setLocale("en-US")
        Preferences.showRawStationIds = false
        assertEquals("Test C", NumberUtils.mapLookup(0x3, mp))
        assertEquals("Unknown (0x77)", NumberUtils.mapLookup(0x77, mp))
        Preferences.showRawStationIds = true
        assertEquals("Test C [0x3]", NumberUtils.mapLookup(0x3, mp))
        assertEquals("Unknown (0x77)", NumberUtils.mapLookup(0x77, mp))
    }
}
