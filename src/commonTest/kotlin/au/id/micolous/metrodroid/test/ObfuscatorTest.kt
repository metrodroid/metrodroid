/*
 * ObfuscatorTest.kt
 *
 * Copyright 2021 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Month
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.TripObfuscator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ObfuscatorTest : BaseInstrumentedTest() {
    @Test
    fun testObfuscator() {
        TripObfuscator.setRandomSourceForTest(Random(43))
        Preferences.obfuscateTripDates = false
        Preferences.obfuscateTripTimes = false
        TimestampFull.nowSource.value = {
            TimestampFull(MetroTimeZone.HELSINKI, 2021, Month.OCTOBER, 5, 13, 21, 37)
        }
        assertEquals(Daystamp(2019, Month.MAY, 10),
            TripObfuscator.maybeObfuscateTS(Daystamp(2019, Month.MAY, 10)))
        assertEquals(TimestampFull(MetroTimeZone.HELSINKI, 2019,
            Month.MAY, 10, 12, 17, 25),
            TripObfuscator.maybeObfuscateTS(TimestampFull(MetroTimeZone.HELSINKI, 2019,
                Month.MAY, 10, 12, 17, 25)))
        Preferences.obfuscateTripDates = true
        Preferences.obfuscateTripTimes = false
        assertEquals(Daystamp(2019, Month.OCTOBER, 11),
            TripObfuscator.maybeObfuscateTS(Daystamp(2019, Month.MAY, 10)))
        assertEquals(Daystamp(2020, Month.OCTOBER, 10),
            TripObfuscator.maybeObfuscateTS(Daystamp(2021, Month.MAY, 10)))
        assertEquals(TimestampFull(MetroTimeZone.HELSINKI, 2019,
            Month.OCTOBER, 11, 12, 17, 25),
            TripObfuscator.maybeObfuscateTS(TimestampFull(MetroTimeZone.HELSINKI, 2019,
                Month.MAY, 10, 12, 17, 25)))
        assertEquals(TimestampFull(MetroTimeZone.HELSINKI, 2020,
            Month.OCTOBER, 10, 12, 17, 25),
            TripObfuscator.maybeObfuscateTS(TimestampFull(MetroTimeZone.HELSINKI, 2021,
                Month.MAY, 10, 12, 17, 25)))
        Preferences.obfuscateTripDates = false
        Preferences.obfuscateTripTimes = true
        assertEquals(Daystamp(2019, Month.MAY, 10),
            TripObfuscator.maybeObfuscateTS(Daystamp(2019, Month.MAY, 10)))
        assertEquals(TimestampFull(MetroTimeZone.HELSINKI, 2019,
            Month.MAY, 10, 16, 4),
            TripObfuscator.maybeObfuscateTS(TimestampFull(MetroTimeZone.HELSINKI, 2019,
                Month.MAY, 10, 12, 17, 25)))
        Preferences.obfuscateTripDates = true
        Preferences.obfuscateTripTimes = true
        assertEquals(Daystamp(2019, Month.OCTOBER, 11),
            TripObfuscator.maybeObfuscateTS(Daystamp(2019, Month.MAY, 10)))
        assertEquals(TimestampFull(MetroTimeZone.HELSINKI, 2019,
            Month.OCTOBER, 11, 11, 58),
            TripObfuscator.maybeObfuscateTS(TimestampFull(MetroTimeZone.HELSINKI, 2019,
                Month.MAY, 10, 12, 17, 25)))
        assertEquals(Daystamp(2020, Month.OCTOBER, 10),
            TripObfuscator.maybeObfuscateTS(Daystamp(2021, Month.MAY, 10)))
        assertEquals(TimestampFull(MetroTimeZone.HELSINKI, 2020,
            Month.OCTOBER, 10, 14, 11),
            TripObfuscator.maybeObfuscateTS(TimestampFull(MetroTimeZone.HELSINKI, 2021,
                Month.MAY, 10, 12, 17, 25)))

        TimestampFull.nowSource.value = {
            TimestampFull(MetroTimeZone.HELSINKI, 2019, Month.DECEMBER, 5, 13, 21, 37)
        }
        assertEquals(Daystamp(2019, Month.OCTOBER, 11),
            TripObfuscator.maybeObfuscateTS(Daystamp(2020, Month.MAY, 9)))
    }
}