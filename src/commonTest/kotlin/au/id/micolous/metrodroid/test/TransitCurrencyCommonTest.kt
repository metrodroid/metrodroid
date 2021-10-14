package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.AUD
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.USD
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.XXX
import au.id.micolous.metrodroid.util.currencyNameByCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Testing [TransitCurrency], but not the (platform-specific) text formatters.
 *
 */
class TransitCurrencyCommonTest:BaseInstrumentedTest() {
    @Test
    fun testNumericLookup() {
        var c = TransitCurrency(1234, 36, 100)
        assertEquals(AUD(1234), c)

        // Test with an invalid code
        c = TransitCurrency(1234, 9999)
        assertEquals(XXX(1234), c)
    }

    @Test
    fun testDivisor() {
        // Test with no divisor -- this should infer the divisor
        var c = TransitCurrency(1234, 36)
        assertEquals(AUD(1234), c)

        // Test with no divisor -- a string currencyCode should NOT infer the divisor
        c = TransitCurrency(1234, "JPY")
        assertEquals(100, c.mDivisor)

        // Test with different divisors for equality
        c = TransitCurrency(12340, "AUD", 1000)
        assertEquals(1000, c.mDivisor)
        assertEquals(AUD(1234), c)

        // Test overriding the divisor in a currency code.
        c = TransitCurrency(12340, 36, 1000)
        assertEquals(1000, c.mDivisor)
        assertEquals(AUD(1234), c)
    }

    @Test
    fun testPlus() {
        assertEquals(AUD(300), AUD(200) + AUD(100))
    }

    @Test
    fun testPlusWrongCurrencies() {
        assertFailsWith(IllegalArgumentException::class) {
            AUD(200) + USD(100)
        }
    }

    @Test
    fun testPlusDenominators() {
        // other is higher, and divisible
        assertExactlyEquals(XXX(10, 10), XXX(1, 2) + XXX(5, 10))

        // other is higher, and not divisible
        assertExactlyEquals(XXX(15, 10), XXX(1, 2) + XXX(5, 5))

        // self is higher, and divisible
        assertExactlyEquals(XXX(10, 10), XXX(5, 10) + XXX(1, 2))

        // self is higher, and not divisible
        assertExactlyEquals(XXX(15, 10), XXX(5, 5) + XXX(1, 2))
    }

    private fun assertExactlyEquals(expected: TransitCurrency, actual: TransitCurrency) {
        assertTrue(expected.exactlyEquals(actual), "$expected is not exactlyEqual to $actual")
    }

    @Test
    fun testEnglishCurrencyName() {
        setLocale("en-US")
        assertEquals(actual=currencyNameByCode(840), expected="US Dollar")
        assertEquals(actual=currencyNameByCode(643), expected="Russian Ruble")
    }

    @Test
    fun testRussianCurrencyName() {
        setLocale("ru-RU")
        assertEquals(actual=currencyNameByCode(840)?.lowercase(), expected="доллар сша")
        assertEquals(actual=currencyNameByCode(643)?.lowercase(), expected="российский рубль")
    }
}
