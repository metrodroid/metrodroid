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

import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.yearToDays
import kotlin.test.Test
import kotlin.test.assertEquals

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
            val ly = year - 1
            val expectedD = year * 365 + ly / 4 - ly / 100 + ly / 400 - 719527
            assertEquals(actual=d, expected=expectedD,
                 message="Wrong days for year $year: $d vs $expectedD")
        }
    }

    @Test
    fun testDaystamp() {
        val monthDays = listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var days = 1600 * 365 + 1599 / 4 - 1599 / 100 + 1599 / 400 - 719527
        for (year in 1600..2600) {
            for (month in 1..12) {
                for (day in 1..monthDays[month-1]) {
                    if (day == 29 && month == 2 && !isBisextile(year))
                        continue
                    val ymd = Daystamp(days)
                    days++
                    assertEquals(
                        actual=ymd.year, expected=year,
                        message="Wrong year for $year-$month-$day vs $ymd"
                    )
                    assertEquals(
                        actual=ymd.monthNumberOneBased, expected=month,
                        message="Wrong month for $year-$month-$day vs $ymd"
                    )
                    assertEquals(
                        actual=ymd.day, expected=day,
                        message="Wrong days for $year-$month-$day vs $ymd"
                    )
                }
            }
        }
    }

    private fun isBisextile(year: Int): Boolean = year % 4 == 0 &&
            (year % 100 != 0 || year % 400 == 0)

    @Test
    fun testRoundTrip() {
        // Cover over 4 years to check bisextile
        for (days in 0..2000) {
            val ymd = Daystamp(days)
            assertEquals (ymd.daysSinceEpoch, days,
                "Wrong roundtrip $days vs ${ymd.daysSinceEpoch}")
        }
    }
}
