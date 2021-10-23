package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals

// This needs to be extended to check more cases
class TimeTest
{
    private fun transitEpochDay(tz: MetroTimeZone, expMillis: Long) {
        val epoch = Epoch.local(1997, tz)
        val ts = epoch.dayMinute(5, 77)
        assertEquals(1997, ts.year)
        assertEquals(Month.JANUARY, ts.month)
        assertEquals(6, ts.day)
        assertEquals(1, ts.hour)
        assertEquals(17, ts.minute)
        assertEquals(expMillis, ts.timeInMillis)
    }

    @Test
    fun testNegative() {
        transitEpochDay(MetroTimeZone.NEW_YORK, 852531420000L)
    }

    @Test
    fun testPositive() {
        transitEpochDay(MetroTimeZone.HELSINKI, 852506220000L)
    }
}
