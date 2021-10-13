/*
 * Timestamp.kt
 *
 * Copyright 2019 Google
 *
 * This file is open to relicensing, if you need it under another
 * license, contact phcoder
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

@file:JvmName("TimestampActualKt")

package au.id.micolous.metrodroid.time

import java.util.*

internal fun makeTimezone(tz: MetroTimeZone) = when (tz) {
    MetroTimeZone.UNKNOWN -> TimeZone.getTimeZone("UTC")
    MetroTimeZone.LOCAL -> TimeZone.getDefault()
    else -> TimeZone.getTimeZone(tz.olson)
}

internal actual fun getMillisFromDays(tz: MetroTimeZone, dhm: DHM): Long {
    val g = GregorianCalendar(makeTimezone(tz))
    g.set(Calendar.YEAR, dhm.yd.year)
    g.set(Calendar.DAY_OF_YEAR, dhm.yd.dayOfYear+1)
    g.set(Calendar.HOUR_OF_DAY, dhm.hour)
    g.set(Calendar.MINUTE, dhm.min)
    g.set(Calendar.SECOND, 0)
    g.set(Calendar.MILLISECOND, 0)
    return g.timeInMillis
}

internal actual fun getDaysFromMillis(millis: Long, tz: MetroTimeZone): DHM {
    val g = GregorianCalendar(makeTimezone(tz))
    g.timeInMillis = millis
    val ymd = YMD(g.get(Calendar.YEAR),
            g.get(Calendar.MONTH),
            g.get(Calendar.DAY_OF_MONTH))
    return DHM(ymd.daysSinceEpoch,
            g.get(Calendar.HOUR_OF_DAY),
            g.get(Calendar.MINUTE))
}

internal actual fun makeNow(): TimestampFull {
    val c = GregorianCalendar.getInstance()
    return TimestampFull(timeInMillis = c.timeInMillis, tz = MetroTimeZone(c.timeZone.id))
}
