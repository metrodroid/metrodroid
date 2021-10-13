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
        assertEquals(1997, ts.ymd.year)
        assertEquals(Month.JANUARY, ts.ymd.month)
        assertEquals(6, ts.ymd.day)
        assertEquals(1, ts.dhm.hour)
        assertEquals(17, ts.dhm.min)
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
