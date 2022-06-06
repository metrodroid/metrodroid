package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderType
import au.id.micolous.metrodroid.transit.smartrider.convertTime
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class SmartRiderTest {
    @Test
    fun testTimestamps() {
        assertEquals(
            TimestampFull(MetroTimeZone.SYDNEY, 2016, Month.OCTOBER, 14, 22, 56, 39),
            convertTime(529800999, SmartRiderType.MYWAY))
        assertEquals(
            TimestampFull(MetroTimeZone.PERTH, 2016, Month.OCTOBER, 14, 22, 56, 39),
            convertTime(529800999, SmartRiderType.SMARTRIDER))
    }
}
