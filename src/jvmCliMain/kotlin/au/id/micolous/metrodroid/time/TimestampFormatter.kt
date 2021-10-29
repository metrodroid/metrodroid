package au.id.micolous.metrodroid.time

import au.id.micolous.metrodroid.multi.FormattedString
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

actual object TimestampFormatter {
    private fun makeCalendar(ts: TimestampFull) =
        makeRawCalendar(ts.adjust())

    private fun makeRawCalendar(ts: TimestampFull) =
        Instant.ofEpochMilli(ts.timeInMillis).atZone(ZoneId.of(ts.tz.resolvedOlson))
    actual fun longDateFormat(ts: Timestamp) = FormattedString(longDateFormat(makeDateCalendar(ts)))

    private fun makeDateCalendar(ts: Timestamp): ZonedDateTime =
        when (ts) {
            is TimestampFull -> makeCalendar(ts)
            is Daystamp -> Instant.ofEpochMilli(
                ts.adjust().daysSinceEpoch * 86400L * 1000L)
                .atZone(UTC)
        }

    actual fun dateTimeFormat(ts: TimestampFull) = FormattedString(dateTimeFormat(makeCalendar(ts)))
    actual fun timeFormat(ts: TimestampFull) = FormattedString(timeFormat(makeDateCalendar(ts)))

    /** Reference to UTC timezone.  */
    private val UTC = ZoneId.of ("Etc/UTC")!!

    private fun formatCalendar(df: DateTimeFormatter, c: ZonedDateTime?): String =
        c?.let { df.format(it) } ?: ""

    private fun longDateFormat(date: ZonedDateTime?): String =
        formatCalendar(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG), date)

    private fun timeFormat(date: ZonedDateTime?): String =
        formatCalendar(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM), date)

    private fun dateTimeFormat(date: ZonedDateTime?): String =
        formatCalendar(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM), date)
}
