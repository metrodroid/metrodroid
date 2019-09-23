/*
 * Timestamp.kt
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

package au.id.micolous.metrodroid.time

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.TripObfuscator
import platform.Foundation.*
import kotlin.native.concurrent.SharedImmutable

fun date2Timestamp(date: NSDate): TimestampFull {
    val t = (date.timeIntervalSince1970 * 1000).toLong()
    val tz = NSTimeZone.defaultTimeZone.name
    return TimestampFull(timeInMillis = t, tz = MetroTimeZone(tz))
}

internal actual fun makeNow(): TimestampFull = date2Timestamp(NSDate())

/** Reference to UTC timezone.  */
@SharedImmutable
private val UTC : NSTimeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)

// Currently empty but there are few time zones that may need mapping in
// the future like e.g. America/Buenos_Aires
@SharedImmutable
private val tzOverrides = mapOf<String,String>()

private fun metroTz2NS(tz: MetroTimeZone): NSTimeZone {
    if (tz == MetroTimeZone.LOCAL)
      return NSTimeZone.defaultTimeZone
    if (tz == MetroTimeZone.UTC || tz == MetroTimeZone.UNKNOWN)
      return UTC
    val tzMapped = tzOverrides[tz.olson] ?: tz.olson
    val nstz = NSTimeZone.timeZoneWithName(tzName = tzMapped)
    if (nstz != null) {
        return nstz
    }
    Log.e("metroTz2NS", "Unable to find timezone ${tz.olson}. Using UTC as fallback but it's likely to result in wrong timestamps")
    return UTC
}

internal actual fun epochDayHourMinToMillis(tz: MetroTimeZone, daysSinceEpoch: Int, hour: Int, min: Int): Long {
    val dateComponents = NSDateComponents()
    val ymd = getYMD(daysSinceEpoch)
    val nstz = metroTz2NS(tz)
    dateComponents.day = ymd.day.toLong()
    dateComponents.month = ymd.month.toLong() + 1
    dateComponents.year = ymd.year.toLong()
    dateComponents.hour = hour.toLong()
    dateComponents.minute = min.toLong()
    dateComponents.second = 0.toLong()
    dateComponents.nanosecond = 0.toLong()
    dateComponents.timeZone = nstz

    val gregorianCalendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
    gregorianCalendar.timeZone = nstz
    val date = gregorianCalendar.dateFromComponents(dateComponents)
    return (date!!.timeIntervalSince1970 * 1000).toLong()
}

internal actual fun getDaysFromMillis(millis: Long, tz: MetroTimeZone): DHM {
    val nstz = metroTz2NS(tz)
    val cal = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
    cal.timeZone = nstz
    val d = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    val comp = cal.componentsInTimeZone(nstz, fromDate = d)
    return DHM(days = YMD(
        year = comp.year.toInt(),
        month = comp.month.toInt() - 1,
        day = comp.day.toInt()).daysSinceEpoch,
        hour = comp.hour.toInt(),
        min = comp.minute.toInt())
}

actual object TimestampFormatter {
    // Equivalent of java Calendar to avoid restructuring the code
    data class Calendar(val time: NSDate, val tz: NSTimeZone)
    fun makeCalendar(ts: TimestampFull): Calendar = makeRawCalendar(ts.adjust())

    private fun makeRawCalendar(ts: TimestampFull): Calendar = Calendar (
        time = NSDate.dateWithTimeIntervalSince1970(ts.timeInMillis / 1000.0),
        tz = metroTz2NS(ts.tz))

    actual fun longDateFormat(ts: Timestamp) = FormattedString(longDateFormat(makeDateCalendar(ts)))

    private fun makeDateCalendar(ts: Timestamp): Calendar =
            when (ts) {
                is TimestampFull -> makeCalendar(ts)
                is Daystamp -> {
                    val adjusted = TripObfuscator.maybeObfuscateTS(ts.adjust())
                    Calendar (
                        time = NSDate.dateWithTimeIntervalSince1970(adjusted.daysSinceEpoch * 86400.0),
                        tz = UTC)
                }
            }

    actual fun dateTimeFormat(ts: TimestampFull) = FormattedString(dateTimeFormat(makeCalendar(ts)))
    fun dateFormat(ts: Timestamp) = FormattedString(dateFormat(makeDateCalendar(ts)))
    actual fun timeFormat(ts: TimestampFull) = FormattedString(timeFormat(makeDateCalendar(ts)))

    private fun formatCalendar(dateStyle: NSDateFormatterStyle,
            timeStyle: NSDateFormatterStyle, c: Calendar): String {
        val dateFormatter = NSDateFormatter()
        dateFormatter.dateStyle = dateStyle
        dateFormatter.timeStyle = timeStyle
        dateFormatter.timeZone = c.tz
 
        return dateFormatter.stringFromDate(c.time)
    }

    private fun longDateFormat(date: Calendar?): String {
        return formatCalendar(NSDateFormatterMediumStyle, NSDateFormatterNoStyle,
                date ?: return "")
    }

    private fun dateFormat(date: Calendar?): String {
        return formatCalendar(NSDateFormatterShortStyle, NSDateFormatterNoStyle,
           date ?: return "")
    }

    private fun timeFormat(date: Calendar?): String {
        return formatCalendar(NSDateFormatterNoStyle, NSDateFormatterShortStyle,
           date ?: return "")
    }

    private fun dateTimeFormat(date: Calendar?): String {
        return formatCalendar(NSDateFormatterShortStyle, NSDateFormatterShortStyle,
           date ?: return "")
    }
}
