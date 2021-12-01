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

package au.id.micolous.metrodroid.time

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.TripObfuscator
import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.native.concurrent.SharedImmutable

@Parcelize
@Serializable(with = MetroTimeZone.Companion::class)
data class MetroTimeZone(val olson: String): Parcelable {
    override fun toString(): String = olson

    val libTimeZone: TimeZone get() = when (this) {
        UNKNOWN -> UTC.libTimeZone
        LOCAL -> TimeZone.currentSystemDefault()
        else -> TimeZone.of(olson)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(forClass = MetroTimeZone::class)
    companion object : KSerializer<MetroTimeZone> {
        override fun serialize(encoder: Encoder, value: MetroTimeZone) {
            encoder.encodeString(value.olson)
        }

        override fun deserialize(decoder: Decoder) = MetroTimeZone(decoder.decodeString())

        // Time zone not specified
        val UNKNOWN = MetroTimeZone(olson = "UNKNOWN")
        // Local device time zone
        val LOCAL = MetroTimeZone(olson = "LOCAL")
        // UTC
        val UTC = MetroTimeZone(olson = "Etc/UTC")
        val ADELAIDE = MetroTimeZone(olson = "Australia/Adelaide")
        val AMSTERDAM = MetroTimeZone(olson = "Europe/Amsterdam")
        val AUCKLAND = MetroTimeZone(olson = "Pacific/Auckland")
        val BEIJING = MetroTimeZone(olson = "Asia/Shanghai")
        val BRISBANE = MetroTimeZone(olson = "Australia/Brisbane")
        val BRUSSELS = MetroTimeZone(olson = "Europe/Brussels")
        val CHICAGO = MetroTimeZone(olson = "America/Chicago")
        val COPENHAGEN = MetroTimeZone(olson = "Europe/Copenhagen")
        val DUBAI = MetroTimeZone(olson = "Asia/Dubai")
        val DUBLIN = MetroTimeZone(olson = "Europe/Dublin")
        val HELSINKI = MetroTimeZone(olson = "Europe/Helsinki")
        val HOUSTON = MetroTimeZone(olson = "America/Chicago")
        val JAKARTA = MetroTimeZone(olson = "Asia/Jakarta")
        val JERUSALEM = MetroTimeZone(olson = "Asia/Jerusalem")
        val JOHANNESBURG = MetroTimeZone(olson ="Africa/Johannesburg")
        val KAMCHATKA = MetroTimeZone(olson = "Asia/Kamchatka")
        val KIEV = MetroTimeZone(olson = "Europe/Kiev")
        val KIROV = MetroTimeZone(olson = "Europe/Kirov")
        val KRASNOYARSK = MetroTimeZone(olson = "Asia/Krasnoyarsk")
        val KUALA_LUMPUR = MetroTimeZone(olson = "Asia/Kuala_Lumpur")
        val LISBON = MetroTimeZone(olson = "Europe/Lisbon")
        val LONDON = MetroTimeZone(olson = "Europe/London")
        val LOS_ANGELES = MetroTimeZone(olson = "America/Los_Angeles")
        val MADRID = MetroTimeZone(olson = "Europe/Madrid")
        val MONTREAL = MetroTimeZone(olson = "America/Montreal")
        val MOSCOW = MetroTimeZone(olson = "Europe/Moscow")
        val NEW_YORK = MetroTimeZone(olson = "America/New_York")
        val OSLO = MetroTimeZone(olson = "Europe/Oslo")
        val PARIS = MetroTimeZone(olson = "Europe/Paris")
        val PERTH = MetroTimeZone(olson = "Australia/Perth")
        val ROME = MetroTimeZone(olson = "Europe/Rome")
        val SAKHALIN = MetroTimeZone(olson = "Asia/Sakhalin")
        val SANTIAGO_CHILE = MetroTimeZone("America/Santiago")
        val SAO_PAULO = MetroTimeZone(olson = "America/Sao_Paulo")
        val SEOUL = MetroTimeZone(olson = "Asia/Seoul")
        val SIMFEROPOL = MetroTimeZone(olson = "Europe/Simferopol")
        val SINGAPORE = MetroTimeZone(olson = "Asia/Singapore")
        val STOCKHOLM = MetroTimeZone(olson = "Europe/Stockholm")
        val SYDNEY = MetroTimeZone(olson = "Australia/Sydney")
        val TAIPEI = MetroTimeZone(olson = "Asia/Taipei")
        val TBILISI = MetroTimeZone(olson = "Asia/Tbilisi")
        val TOKYO = MetroTimeZone(olson = "Asia/Tokyo")
        val VANCOUVER = MetroTimeZone(olson = "America/Vancouver")
        val VLADIVOSTOK = MetroTimeZone(olson = "Asia/Vladivostok")
        val YEKATERINBURG = MetroTimeZone(olson = "Asia/Yekaterinburg")
        val SAMARA = MetroTimeZone(olson = "Europe/Samara")
        val YAKUTSK = MetroTimeZone(olson = "Asia/Yakutsk")
        val OMSK = MetroTimeZone(olson = "Asia/Omsk")
        val NOVOSIBIRSK = MetroTimeZone(olson = "Asia/Novosibirsk")
        val NOVOKUZNETSK = MetroTimeZone(olson = "Asia/Novokuznetsk")
        val WARSAW = MetroTimeZone(olson = "Europe/Warsaw")
    }
}

internal const val SEC = 1000L
internal const val MIN = 60L * SEC
internal const val HOUR = 60L * MIN
internal const val DAY = 24L * HOUR

@SharedImmutable
val epochLocalDate = LocalDate(1970, Month.JANUARY, 1)

fun yearToDays(year: Int): Int = epochLocalDate.daysUntil(
    LocalDate(year, Month.JANUARY, 1)
)

internal fun yearToMillis(year: Int) = yearToDays(year) * DAY

typealias Month = kotlinx.datetime.Month

interface Duration {
    fun addFull (ts: TimestampFull): TimestampFull
    companion object {
        fun daysLocal(d: Int) = DurationPeriodLocal(
            DatePeriod(0, 0, d))
        fun mins(m: Int) = DurationSec(m * 60)
        fun yearsLocal(y: Int) = DurationPeriodLocal(
            DatePeriod(y, 0, 0))
        fun monthsLocal(m: Int) = DurationPeriodLocal(
            DatePeriod(0, m, 0))
    }
}

interface DayDuration : Duration {
    fun addDays(ts: Daystamp): Daystamp
    fun addAny (ts: Timestamp): Timestamp = when (ts) {
        is Daystamp -> addDays(ts)
        is TimestampFull -> addFull(ts)
    }
}

class DurationPeriodLocal(private val d: DatePeriod) : DayDuration {
    override fun addFull(ts: TimestampFull) = ts + d

    override fun addDays(ts: Daystamp) = ts + d
}

class DurationSec(private val s: Int) : Duration {
    override fun addFull(ts: TimestampFull) = TimestampFull(
            timeInMillis = ts.timeInMillis + s * SEC,
            tz = ts.tz)
}

interface Epoch : Parcelable {
    companion object {
        fun utc(year: Int, tz: MetroTimeZone, minOffset: Int = 0) = EpochUTC(
                baseDays = yearToDays(year),
                baseMillis = yearToMillis(year) + minOffset * MIN, outputTz = tz)
        fun local(year: Int, tz: MetroTimeZone) = EpochLocal(yearToDays(year), tz)
    }
}

// This is used when timestamps observe regular progression without any DST
@Parcelize
class EpochUTC internal constructor(private val baseMillis: Long,
                                    private val baseDays: Int,
                                    private val outputTz: MetroTimeZone) : Epoch {
    fun mins(offset: Int) =
            TimestampFull(timeInMillis = baseMillis + offset * MIN, tz = outputTz)
    fun days(offset: Int) =
            Daystamp(daysSinceEpoch = baseDays + offset)

    fun seconds(offset: Long) =
            TimestampFull(timeInMillis = baseMillis + offset * SEC, tz = outputTz)
    fun dayMinute(d: Int, m: Int) = TimestampFull(
            timeInMillis = baseMillis + d * DAY + m * MIN, tz = outputTz)
    fun daySecond(d: Int, s: Int) = TimestampFull(
            timeInMillis = baseMillis + d * DAY + s * SEC, tz = outputTz)

    fun dayHourMinuteSecond(d: Int, h: Int, m: Int, s: Int) = TimestampFull(
            timeInMillis = baseMillis + d * DAY + h * HOUR + m * MIN + s * SEC, tz = outputTz)
}

//The nasty timestamps: day is equal to calendar day
@Parcelize
class EpochLocal internal constructor(private val baseDays: Int,
                                      private val tz: MetroTimeZone) : Epoch {
    fun days(d: Int) =
            Daystamp(daysSinceEpoch = baseDays + d)

    fun dayHMS(d: Int, h: Int, m: Int, s: Int = 0) =
        if (h in 0..23 && m in 0..59 && s in 0..59)
            TimestampFull(
                tz = tz,
                localDateTime = days(d).localDate.atTime(hour = h, minute = m, second = s)
            )
        else
            TimestampFull(
                tz = tz,
                timeInMillis = days(d).localDate.atTime(hour = 0, minute = 0, second = 0)
                    .toInstant(timeZone = tz.libTimeZone).toEpochMilliseconds() + (h * 3600L + m * 60L + s) * 1000L)

    fun dayMinute(d: Int, m: Int) = dayHMS(d = d, h = m / 60, m = m % 60)
    fun daySecond(d: Int, s: Int) = dayHMS(d = d, h = s / 3600,
        m = (s / 60) % 60, s = s % 60)
}

sealed class Timestamp: Parcelable {
    val monthNumberOneBased: Int get() = month.number
    val monthNumberZeroBased: Int get() = month.number - 1
    val month: Month get() = localDate.month
    val year: Int get() = localDate.year
    val day: Int get() = localDate.dayOfMonth

    abstract val localDate: LocalDate
    abstract fun format(): FormattedString
    open operator fun plus(duration: DayDuration): Timestamp = duration.addAny(this)
    abstract fun toDaystamp(): Daystamp
    abstract fun obfuscateDelta(delta: Long): Timestamp
    abstract fun plus(duration: DatePeriod): Timestamp

    fun isSameDay(other: Timestamp): Boolean = this.toDaystamp() == other.toDaystamp()
}

@Parcelize
@Serializable
// Only date is known
data class Daystamp internal constructor(val daysSinceEpoch: Int): Timestamp(), Comparable<Daystamp> {
    override fun toDaystamp(): Daystamp = this

    override fun compareTo(other: Daystamp): Int = daysSinceEpoch.compareTo(other.daysSinceEpoch)

    override fun obfuscateDelta(delta: Long) = Daystamp(daysSinceEpoch = daysSinceEpoch + ((delta + DAY/2) / DAY).toInt())

    override fun format(): FormattedString =
                TimestampFormatter.longDateFormat(this)

    val dayOfYear: Int get() = localDate.dayOfYear
    override val localDate by lazy {
        epochLocalDate + DatePeriod(0, 0, daysSinceEpoch)
    }

    override operator fun plus(duration: DatePeriod) = Daystamp(
        localDate + duration)

    fun adjust() : Daystamp = this
    fun promote(tz: MetroTimeZone, hour: Int, min: Int): TimestampFull = TimestampFull(
            tz = tz, localDateTime = localDate.atTime(hour, min))

    /**
     * Formats a GregorianCalendar in to ISO8601 date format in local time (ie: without any timezone
     * conversion).  This is designed for [Daystamp] values which only have a valid date
     * component.
     *
     * This should only be used for debugging logs, in order to ensure consistent
     * information.
     *
     * @receiver Date to format
     * @return String representing the date in ISO8601 format.
     */
    private fun isoDateFormat(): String {
        // ISO_DATE_FORMAT = SimpleDateFormat ("yyyy-MM-dd", Locale.US)
        return NumberUtils.zeroPad(localDate.year, 4) + "-" +
                NumberUtils.zeroPad(localDate.month.number, 2) + "-" +
                NumberUtils.zeroPad(localDate.dayOfMonth, 2)
    }

    override fun toString(): String = isoDateFormat()

    /**
     * Represents a year, month and day in the Gregorian calendar.
     *
     * @param month Month, where January = 0.
     * @param day Day of the month, where the first day of the month = 1.
     */
    constructor(year: Int, month: Int, day: Int) : this(
        LocalDate(1600, Month.JANUARY, 1)
                + DatePeriod(year - 1600, month, day - 1))

    constructor(year: Int, month: Month, day: Int) : this(
        year, month.number - 1, day)

    constructor(localDate: LocalDate) : this(
        daysSinceEpoch = epochLocalDate.daysUntil(localDate)
    )

    companion object {
        fun fromDayOfYear(year: Int, dayOfYear: Int) =
            Daystamp(LocalDate(year, Month.JANUARY, 1) + DatePeriod(0, 0, dayOfYear))
    }
}

@Parcelize
@Serializable
// Precision or minutes and higher
data class TimestampFull(val timeInMillis: Long,
                                            val tz: MetroTimeZone): Parcelable, Comparable<TimestampFull>, Timestamp() {
    override fun toDaystamp() = Daystamp(ldt.date)

    val hour: Int get() = ldt.hour
    val minute: Int get() = ldt.minute
    val ldt by lazy {
        Instant.fromEpochMilliseconds(timeInMillis).toLocalDateTime(tz.libTimeZone)
    }
    val ldtUtc by lazy {
        Instant.fromEpochMilliseconds(timeInMillis).toLocalDateTime(MetroTimeZone.UTC.libTimeZone)
    }
    override val localDate by lazy {
        ldt.date
    }

    override fun compareTo(other: TimestampFull): Int = timeInMillis.compareTo(other = other.timeInMillis)
    fun adjust() : TimestampFull =
            TripObfuscator.maybeObfuscateTS(if (Preferences.convertTimezone)
                TimestampFull(timeInMillis, MetroTimeZone.LOCAL) else this)

    operator fun plus(duration: Duration) = duration.addFull(this)
    override operator fun plus(duration: DayDuration) = duration.addFull(this)
    override operator fun plus(duration: DatePeriod) = TimestampFull(
        tz = tz,
        localDateTime = (ldt.date + duration).atTime(ldt.hour, ldt.minute,
            ldt.second, ldt.nanosecond))

    override fun format(): FormattedString = TimestampFormatter.dateTimeFormat(this)

    constructor(tz : MetroTimeZone, year: Int, month: Int, day: Int, hour: Int,
                min: Int, sec: Int = 0) : this(
            tz = tz,
            localDateTime = (LocalDate(1600, Month.JANUARY, 1)
                        + DatePeriod(year - 1600, month, day - 1))
                    .atTime(hour, min, sec)
    )

    constructor(tz : MetroTimeZone, year: Int, month: Month, day: Int, hour: Int,
                min: Int, sec: Int = 0) : this(
        year = year, month = month.number - 1, day = day,
        hour = hour, min = min, sec = sec,
        tz = tz
    )

    constructor(tz: MetroTimeZone, localDateTime: LocalDateTime) : this(
        tz = tz,
        timeInMillis = localDateTime.toInstant(tz.libTimeZone).toEpochMilliseconds()
    )

    /**
     * Formats a GregorianCalendar in to ISO8601 date and time format in UTC. This should only be
     * used for debugging logs, in order to ensure consistent information.
     *
     * @receiver Date/time to format
     * @return String representing the date and time in ISO8601 format.
     */
    fun isoDateTimeFormat(): String {
        //  SimpleDateFormat ("yyyy-MM-dd HH:mm", Locale.US)
        return NumberUtils.zeroPad(ldtUtc.year, 4) + "-" +
                NumberUtils.zeroPad(ldtUtc.month.number, 2) + "-" +
                NumberUtils.zeroPad(ldtUtc.dayOfMonth, 2) + " " +
                NumberUtils.zeroPad(ldtUtc.hour, 2) + ":" +
                NumberUtils.zeroPad(ldtUtc.minute, 2)
    }

    /**
     * Formats a GregorianCalendar in to ISO8601 date and time format in UTC, but with only
     * characters that can be used in filenames on most filesystems.
     *
     * @receiver Date/time to format
     * @return String representing the date and time in ISO8601 format.
     */
    fun isoDateTimeFilenameFormat(): String {
        //  SimpleDateFormat ("yyyyMMdd-HHmmss", Locale.US)
        return NumberUtils.zeroPad(ldtUtc.year, 4) +
                NumberUtils.zeroPad(ldtUtc.month.number, 2) +
                NumberUtils.zeroPad(ldtUtc.dayOfMonth, 2) + "-" +
                NumberUtils.zeroPad(ldtUtc.hour, 2) +
                NumberUtils.zeroPad(ldtUtc.minute, 2) +
                NumberUtils.zeroPad(ldtUtc.second, 2)
    }

    override fun toString(): String = isoDateTimeFormat() + "/$tz"

    override fun obfuscateDelta(delta: Long) = TimestampFull(timeInMillis = timeInMillis + delta, tz = tz)

    companion object {
        fun now() = TimestampFull(
                timeInMillis = Clock.System.now().toEpochMilliseconds(),
                tz = MetroTimeZone(TimeZone.currentSystemDefault().id))
    }
}
