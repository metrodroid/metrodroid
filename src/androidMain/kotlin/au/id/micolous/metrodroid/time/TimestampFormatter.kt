/*
 * TimestampFormatter.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

import android.os.Build
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.LocaleSpan
import android.text.style.TtsSpan
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.TripObfuscator
import java.text.SimpleDateFormat
import java.util.*

actual object TimestampFormatter {
    private fun makeTimezone(tz: MetroTimeZone): TimeZone = TimeZone.getTimeZone(tz.resolvedOlson)
    private fun makeCalendar(ts: TimestampFull): Calendar = makeRawCalendar(ts.adjust())

    private fun makeRawCalendar(ts: TimestampFull): Calendar {
        val g = GregorianCalendar(makeTimezone(ts.tz))
        g.timeInMillis = ts.timeInMillis
        return g
    }

    actual fun longDateFormat(ts: Timestamp) = FormattedString(longDateFormat(makeDateCalendar(ts)))

    private fun makeDateCalendar(ts: Timestamp): Calendar =
            when (ts) {
                is TimestampFull -> makeCalendar(ts)
                is Daystamp -> {
                    val adjusted = TripObfuscator.maybeObfuscateTS(ts.adjust())
                    val g = GregorianCalendar(UTC)
                    g.timeInMillis = 0
                    g.add(Calendar.DAY_OF_YEAR, adjusted.daysSinceEpoch)
                    g
                }
            }

    actual fun dateTimeFormat(ts: TimestampFull) = FormattedString(dateTimeFormat(makeCalendar(ts)))
    fun dateFormat(ts: Timestamp) = FormattedString(dateFormat(makeDateCalendar(ts)))
    actual fun timeFormat(ts: TimestampFull) = FormattedString(timeFormat(makeDateCalendar(ts)))

    /** Reference to UTC timezone.  */
    private val UTC : TimeZone = TimeZone.getTimeZone("Etc/UTC")

    private fun formatCalendar(df: java.text.DateFormat, c: Calendar): String {
        df.timeZone = c.timeZone
        return df.format(c.time)
    }

    private fun longDateFormat(date: Calendar?): SpannableString {
        val df = if (Preferences.useIsoDateTimeStamps)
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        else
            DateFormat.getLongDateFormat(MetrodroidApplication.instance)
        val s = formatCalendar(df,
                date ?: return SpannableString(""))

        val b = SpannableString(s)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setSpan(TtsSpan.DateBuilder()
                    .setYear(date.get(Calendar.YEAR))
                    .setMonth(date.get(Calendar.MONTH))
                    .setDay(date.get(Calendar.DAY_OF_MONTH))
                    .setWeekday(date.get(Calendar.DAY_OF_WEEK)), 0, b.length, 0)
            b.setSpan(LocaleSpan(Locale.getDefault()), 0, b.length, 0)
        }

        return b
    }

    private fun dateFormat(date: Calendar?): SpannableString {
        val df = if (Preferences.useIsoDateTimeStamps)
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        else
            DateFormat.getDateFormat(MetrodroidApplication.instance)
        val s = formatCalendar(df,
                date ?: return SpannableString(""))

        val b = SpannableString(s)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setSpan(TtsSpan.DateBuilder()
                    .setYear(date.get(Calendar.YEAR))
                    .setMonth(date.get(Calendar.MONTH))
                    .setDay(date.get(Calendar.DAY_OF_MONTH)), 0, b.length, 0)
            b.setSpan(LocaleSpan(Locale.getDefault()), 0, b.length, 0)
        }

        return b
    }

    private fun timeFormat(date: Calendar?): SpannableString {
        val tf = if (Preferences.useIsoDateTimeStamps)
            SimpleDateFormat("HH:mm", Locale.US)
        else
            DateFormat.getTimeFormat(MetrodroidApplication.instance)
        val s = formatCalendar(tf,
                date ?: return SpannableString(""))

        val b = SpannableString(s)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setSpan(TtsSpan.TimeBuilder(
                    date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE)), 0, b.length, 0)
            b.setSpan(LocaleSpan(Locale.getDefault()), 0, b.length, 0)
        }
        return b
    }

    private fun dateTimeFormat(date: Calendar?): SpannableString {
        val df = if (Preferences.useIsoDateTimeStamps)
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        else
            DateFormat.getDateFormat(MetrodroidApplication.instance)
        val d =  formatCalendar(df,
                date ?: return SpannableString(""))
        val tf = if (Preferences.useIsoDateTimeStamps)
            SimpleDateFormat("HH:mm", Locale.US)
        else
            DateFormat.getTimeFormat(MetrodroidApplication.instance)
        val t = formatCalendar(tf, date)
        val separator = if (Preferences.useIsoDateTimeStamps) "T" else " "

        val b = SpannableString("$d$separator$t")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setSpan(TtsSpan.DateBuilder()
                    .setYear(date.get(Calendar.YEAR))
                    .setMonth(date.get(Calendar.MONTH))
                    .setDay(date.get(Calendar.DAY_OF_MONTH)), 0, d.length, 0)

            b.setSpan(TtsSpan.TimeBuilder(
                    date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE)), d.length + 1, b.length, 0)

            b.setSpan(LocaleSpan(Locale.getDefault()), 0, b.length, 0)
        }

        return b
    }
}
