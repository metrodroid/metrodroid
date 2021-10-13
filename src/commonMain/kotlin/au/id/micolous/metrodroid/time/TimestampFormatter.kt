package au.id.micolous.metrodroid.time

import au.id.micolous.metrodroid.multi.FormattedString

expect object TimestampFormatter {
    fun longDateFormat(ts: Timestamp): FormattedString
    fun dateTimeFormat(ts: TimestampFull): FormattedString
    fun timeFormat(ts: TimestampFull): FormattedString
}
