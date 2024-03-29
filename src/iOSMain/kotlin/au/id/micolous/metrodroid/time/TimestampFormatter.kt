/*
 * TimestampFormatter.kt
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
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.TripObfuscator
import platform.Foundation.*

actual object TimestampFormatter {

    /** Reference to UTC timezone.  */
    private val UTC: NSTimeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)

    // Currently empty but there are few time zones that may need mapping in
    // the future like e.g. America/Buenos_Aires
    private val tzOverrides = mapOf<String, String>()

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
        Log.e(
            "metroTz2NS",
            "Unable to find timezone ${tz.olson}. Using UTC as fallback but it's likely to result in wrong timestamps"
        )
        return UTC
    }

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
    @Suppress("unused") // Used from Swift
    fun dateFormat(ts: Timestamp) = FormattedString(dateFormat(makeDateCalendar(ts)))
    actual fun timeFormat(ts: TimestampFull) = FormattedString(timeFormat(makeDateCalendar(ts)))

    private fun formatCalendar(dateStyle: NSDateFormatterStyle,
            timeStyle: NSDateFormatterStyle, c: Calendar): String {
        val dateFormatter = NSDateFormatter()
        if (Preferences.useIsoDateTimeStamps) {
            dateFormatter.locale = NSLocale(localeIdentifier = "en_US_POSIX")
            when {
                timeStyle == NSDateFormatterNoStyle -> dateFormatter.dateFormat = "yyyy-MM-dd"
                dateStyle == NSDateFormatterNoStyle -> dateFormatter.dateFormat = "HH:mm"
                else -> dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm"
            }
        } else {
            dateFormatter.dateStyle = dateStyle
            dateFormatter.timeStyle = timeStyle
        }
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
