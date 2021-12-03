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
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ObfuscatedTrip
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.TripObfuscator
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObfuscatorTest : BaseInstrumentedTest() {
    @BeforeTest
    fun setUp() {
        TripObfuscator.randomSource = Random(43)
        Preferences.obfuscateTripDates = false
        Preferences.obfuscateTripTimes = false
        TimestampFull.nowSource.value = {
            TimestampFull(MetroTimeZone.HELSINKI, 2021, Month.OCTOBER, 5, 13, 21, 37)
        }
    }

    @Test
    fun testTimestamps() {
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

    @Test
    fun testTrip() {
        val orig = ObfuscatedTrip(
            mode = Trip.Mode.BUS,
            passengerCount = 1,
            startTimestamp = TimestampFull(MetroTimeZone.HELSINKI, 2021,
                Month.MAY, 10, 12, 17, 25),
            endTimestamp = TimestampFull(MetroTimeZone.HELSINKI, 2021,
                Month.MAY, 10, 13, 41, 53),
            fare = TransitCurrency.EUR(325),
            rawFieldsUnknown = "unknown fields",
            rawFieldsFull = "full fields"
        )
        val orig2 = ObfuscatedTrip(
            mode = Trip.Mode.BUS,
            passengerCount = 1
        )
        val obfuscated = ObfuscatedTrip(
            mode = Trip.Mode.BUS,
            passengerCount = 1,
            startTimestamp = TimestampFull(MetroTimeZone.HELSINKI, 2020,
                Month.OCTOBER, 10, 16, 4),
            endTimestamp = TimestampFull(MetroTimeZone.HELSINKI, 2020,
                Month.OCTOBER, 10, 17, 28, 28),
            fare = TransitCurrency.EUR(260)
        )

        assertEquals(listOf(orig, orig2), TransitData.prepareTripsSafeReal(listOf(orig, orig2)))

        assertNull(orig.getRawFields(TransitData.RawLevel.NONE))

        Preferences.obfuscateTripTimes = true
        assertNull(orig.getRawFields(TransitData.RawLevel.NONE))
        assertNull(orig.getRawFields(TransitData.RawLevel.UNKNOWN_ONLY))
        assertNull(orig.getRawFields(TransitData.RawLevel.ALL))
        Preferences.obfuscateTripTimes = false

        Preferences.obfuscateTripDates = true
        assertNull(orig.getRawFields(TransitData.RawLevel.NONE))
        assertNull(orig.getRawFields(TransitData.RawLevel.UNKNOWN_ONLY))
        assertNull(orig.getRawFields(TransitData.RawLevel.ALL))
        Preferences.obfuscateTripDates = false

        Preferences.obfuscateTripFares = true
        assertNull(orig.getRawFields(TransitData.RawLevel.NONE))
        assertNull(orig.getRawFields(TransitData.RawLevel.UNKNOWN_ONLY))
        assertNull(orig.getRawFields(TransitData.RawLevel.ALL))
        Preferences.obfuscateTripFares = false

        Preferences.obfuscateBalance = true
        assertNull(orig.getRawFields(TransitData.RawLevel.NONE))
        assertNull(orig.getRawFields(TransitData.RawLevel.UNKNOWN_ONLY))
        assertNull(orig.getRawFields(TransitData.RawLevel.ALL))
        Preferences.obfuscateBalance = false

        Preferences.hideCardNumbers = true
        assertNull(orig.getRawFields(TransitData.RawLevel.NONE))
        assertNull(orig.getRawFields(TransitData.RawLevel.UNKNOWN_ONLY))
        assertNull(orig.getRawFields(TransitData.RawLevel.ALL))
        Preferences.hideCardNumbers = false

        Preferences.obfuscateTripTimes = true
        Preferences.obfuscateTripDates = true
        Preferences.obfuscateTripFares = true

        assertEquals(listOf(obfuscated, orig2), TransitData.prepareTripsSafeReal(listOf(orig, orig2)))
    }
}