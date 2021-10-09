/*
 * DateTest.kt
 *
 * Copyright 2019 Google
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

import au.id.micolous.metrodroid.time.getYMD
import au.id.micolous.metrodroid.time.yearToDays
import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.*

/**
 * Testing the Date functions
 */
class DateTest {
    @Test
    fun testYearToDays() {
            // Before 1600 java calendar switches to Julian calendar.
        // I don't think there were any contactless payment cards for horse
        // carriages, so we don't care
        for (year in 1600..10000) {
            val d = yearToDays(year)
            val g = GregorianCalendar(TimeZone.getTimeZone("UTC"))
            g.timeInMillis = 0
            g.set(Calendar.YEAR, year)
            val expectedD = g.timeInMillis / (86400L * 1000L)
            assertEquals(d, expectedD.toInt(),
                 "Wrong days for year $year: $d vs $expectedD")
        }
    }

    @Test
    fun testGetYMD() {
        // years 1697 to 2517
        for (days in (-100000)..200000) {
            val ymd = getYMD(days)
            val g = GregorianCalendar(TimeZone.getTimeZone("UTC"))
            g.timeInMillis = days * 86400L * 1000L
            val expectedY = g.get(Calendar.YEAR)
            val expectedM = g.get(Calendar.MONTH)
            val expectedD = g.get(Calendar.DAY_OF_MONTH)
            assertEquals (ymd.year, expectedY,
                "Wrong year for days $days: ${ymd.year} vs $expectedY")
            assertEquals (ymd.month.zeroBasedIndex, expectedM,
                "Wrong month for days $days: ${ymd.month} vs $expectedM")
            assertEquals (ymd.day, expectedD,
            "Wrong days for days $days: ${ymd.day} vs $expectedD")
        }
    }

    @Test
    fun testRoundTrip() {
        // Cover over 4 years to check bisextile
        for (days in 0..2000) {
            val ymd = getYMD(days)
            assertEquals (ymd.daysSinceEpoch, days,
                "Wrong roundtrip $days vs ${ymd.daysSinceEpoch}")
        }
    }
}
