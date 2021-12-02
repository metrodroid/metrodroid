package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.NumberUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
}