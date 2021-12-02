package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.ifFalse
import au.id.micolous.metrodroid.util.ifTrue
import au.id.micolous.metrodroid.util.plusAssign
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
}
