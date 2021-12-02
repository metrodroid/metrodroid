package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.plusAssign
import kotlin.test.Test
import kotlin.test.assertEquals

class MiscTest {
    @Test
    fun testPlusAssign() {
        val sb = StringBuilder("ABC")
        sb += "DEF"
        assertEquals("ABCDEF", sb.toString())
    }
}