package au.id.micolous.metrodroid.time

import au.id.micolous.metrodroid.multi.FormattedString
import java.util.*
import java.text.DateFormat

actual object TimestampFormatter {
    private fun makeCalendar(ts: TimestampFull): Calendar = makeRawCalendar(ts.adjust())

    private fun makeRawCalendar(ts: TimestampFull): Calendar {
        val g = GregorianCalendar(makeTimezone(ts.tz))
        g.timeInMillis = ts.timeInMillis
        return g
    }

    actual fun longDateFormat(ts: Timestamp) = FormattedString(longDateFormat(makeDateCalendar(ts)))

    private fun makeDateCalendar(ts: Timestamp): Calendar {
        when (ts) {
            is TimestampFull -> return makeCalendar(ts)
            is Daystamp -> {
                val adjusted = ts.adjust()
                val g = GregorianCalendar(UTC)
                g.timeInMillis = 0
                g.add(Calendar.DAY_OF_YEAR, adjusted.daysSinceEpoch)
                return g
            }
        }
    }

    actual fun dateTimeFormat(ts: TimestampFull) = FormattedString(dateTimeFormat(makeCalendar(ts)))
    actual fun timeFormat(ts: TimestampFull) = FormattedString(timeFormat(makeDateCalendar(ts)))

    /** Reference to UTC timezone.  */
    val UTC = TimeZone.getTimeZone("Etc/UTC")!!

    private fun formatCalendar(df: DateFormat, c: Calendar): String {
        df.timeZone = c.timeZone
        return df.format(c.time)
    }

    private fun longDateFormat(date: Calendar?): String {
        return formatCalendar(DateFormat.getDateInstance(DateFormat.LONG), date ?: return "")
    }

    private fun timeFormat(date: Calendar?): String {
        return formatCalendar(DateFormat.getTimeInstance(), date ?: return "")
    }

    private fun dateTimeFormat(date: Calendar?): String {
        return formatCalendar(DateFormat.getDateTimeInstance(), date ?: return "")
    }
}
