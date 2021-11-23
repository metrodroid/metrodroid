package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.ISO3166
import kotlin.test.Test
import kotlin.test.assertEquals

class ISO3166Test {
    @Test
    fun testISO3166() {
        assertEquals(expected="CH", actual=ISO3166.mapNumericToAlpha2(756))
        assertEquals(expected="US", actual=ISO3166.mapNumericToAlpha2(840))
        assertEquals(expected="AU", actual=ISO3166.mapNumericToAlpha2(36))
    }
}