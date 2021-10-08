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
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.native.concurrent.SharedImmutable

@Parcelize
@Serializable(with = MetroTimeZone.Companion::class)
data class MetroTimeZone(val olson: String): Parcelable {
    override fun toString(): String = olson

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
        val BRUXELLES = MetroTimeZone(olson = "Europe/Brussels")
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

data class DHM(val days: Int, val hour: Int, val min: Int) {
    val yd: YD
        get() = getYD(days)
}

internal expect fun makeNow(): TimestampFull
internal expect fun epochDayHourMinToMillis(tz: MetroTimeZone, daysSinceEpoch: Int, hour: Int, min: Int): Long
internal expect fun getDaysFromMillis(millis: Long, tz: MetroTimeZone): DHM
internal const val SEC = 1000L
internal const val MIN = 60L * SEC
internal const val HOUR = 60L * MIN
internal const val DAY = 24L * HOUR

fun isBisextile(year: Int): Boolean = (year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0)

data class YD(val year: Int, val dayOfYear: Int) {
    val daysSinceEpoch: Int = yearToDays(year) + dayOfYear
}

fun getYD(daysSinceEpoch: Int): YD {
    val daysSinceX = daysSinceEpoch + 719162
    // 400 years has 97 bisextile years
    val groups400y = daysSinceX / (400 * 365 + 97)
    val remainder400y = daysSinceX % (400 * 365 + 97)
    val groups100y = minOf(remainder400y / (100 * 365 + 24), 3)
    val remainder100y = remainder400y - (100 * 365 + 24) * groups100y
    val groups4y = remainder100y / (365 * 4 + 1)
    val remainder4y = remainder100y % (365 * 4 + 1)
    val group1y = minOf(remainder4y / 365, 3)
    val remainder1y = remainder4y - group1y * 365
    val y = 1 + groups400y * 400 + groups100y * 100 + groups4y * 4 + group1y
    return YD(y, remainder1y)
}

fun getYMD(daysSinceEpoch: Int): YMD {
    val (y, dy) = getYD(daysSinceEpoch)
    val correctionD = if (!isBisextile(y) && dy >= 31 + 28) 1 else 0
    val correctedDays = dy + correctionD

    val (m, d) = when (correctedDays) {
        in 0..30 -> Pair(Month.JANUARY, correctedDays + 1)
        in 31..59 -> Pair(Month.FEBRUARY, correctedDays - 30)
        in 60..90 -> Pair(Month.MARCH, correctedDays - 59)
        in 91..120 -> Pair(Month.APRIL, correctedDays - 90)
        in 121..151 -> Pair(Month.MAY, correctedDays - 120)
        in 152..181 -> Pair(Month.JUNE, correctedDays - 151)
        in 182..212 -> Pair(Month.JULY, correctedDays - 181)
        in 213..243 -> Pair(Month.AUGUST, correctedDays - 212)
        in 244..273 -> Pair(Month.SEPTEMBER, correctedDays - 243)
        in 274..304 -> Pair(Month.OCTOBER, correctedDays - 273)
        in 305..334 -> Pair(Month.NOVEMBER, correctedDays - 304)
        else -> Pair(Month.DECEMBER, correctedDays - 334)
    }

    return YMD(year = y, month = m, day = d)
}

fun yearToDays(year: Int): Int {
    val offYear = year - 1
    var days = offYear * 365
    days += offYear / 4
    days -= offYear / 100
    days += offYear / 400
    return days - 719162
}

@SharedImmutable
private val monthToDays = listOf(0, 31, 59, 90, 120, 151, 181,
        212, 243, 273, 304, 334)

/**
 * Enum of 0-indexed months in the Gregorian calendar
 */
enum class Month(val zeroBasedIndex: Int) {
    JANUARY(0),
    FEBRUARY(1),
    MARCH(2),
    APRIL(3),
    MAY(4),
    JUNE(5),
    JULY(6),
    AUGUST(7),
    SEPTEMBER(8),
    OCTOBER(9),
    NOVEMBER(10),
    DECEMBER(11);

    val oneBasedIndex: Int get() = zeroBasedIndex + 1

    companion object {
        fun zeroBased(idx: Int): Month = values()[idx]
    }
}

/**
 * Represents a year, month and day in the Gregorian calendar.
 *
 * @property month Month, where January = Month.JANUARY.
 * @property day Day of the month, where the first day of the month = 1.
 */
data class YMD(val year: Int, val month: Month, val day: Int) {
    constructor(other: YMD): this(other.year, other.month, other.day)
    constructor(year: Int, month: Int, day: Int) : this(normalize(year, month, day))

    val daysSinceEpoch: Int get() = countDays(year, month.zeroBasedIndex, day)

    companion object {
        private fun countDays(year: Int, month: Int, day: Int): Int {
            val ym = 12 * year + month
            var y: Int = ym / 12
            var m: Int = ym % 12
            // We don't really care for dates before 1 CE
            // but at least we shouldn't crash on them
            // This code results in astronomical year numbering
            // that includes year 0
            if (m < 0) {
                m += 12
                y -= 1
            }
            return yearToDays(y) + (day - 1) + monthToDays[m] + (
                if (isBisextile(y) && m > Month.FEBRUARY.zeroBasedIndex) 1 else 0)
        }
        private fun normalize(year: Int, month: Int, day: Int): YMD =
            getYMD(countDays(year, month, day))
    }    
}

internal fun yearToMillis(year: Int) = yearToDays(year) * DAY

fun addYearToDays(from: Int, years: Int): Int {
    val ymd = getYMD(from)
    return YMD(ymd.year + years, ymd.month, ymd.day).daysSinceEpoch
}

fun addMonthToDays(from: Int, months: Int): Int {
    val ymd = getYMD(from)
    return YMD(ymd.year, ymd.month.zeroBasedIndex + months, ymd.day).daysSinceEpoch
}

internal fun addYearToMillis(from: Long, tz: MetroTimeZone, years: Int): Long {
    val ms = from % MIN
    val (days, h, m) = getDaysFromMillis(from, tz)
    return epochDayHourMinToMillis(tz = tz,
            daysSinceEpoch = addYearToDays(from = days, years = years),
            hour = h, min = m) + ms
}

internal fun addMonthToMillis(from: Long, tz: MetroTimeZone, months: Int): Long {
    val ms = from % MIN
    val (days, h, m) = getDaysFromMillis(from, tz)
    return epochDayHourMinToMillis(tz = tz,
            daysSinceEpoch = addMonthToDays(from = days, months = months),
            hour = h, min = m) + ms
}

internal fun addDay(from: Long, tz: MetroTimeZone, days: Int): Long {
    val ms = from % MIN
    val (d, h, m) = getDaysFromMillis(from, tz)
    return epochDayHourMinToMillis(tz = tz,
            daysSinceEpoch = d + days,
            hour = h, min = m) + ms
}

interface Duration {
    fun addFull (ts: TimestampFull): TimestampFull
    companion object {
        fun daysLocal(d: Int) = DurationDaysLocal(d)
        fun mins(m: Int) = DurationSec(m * 60)
        fun yearsLocal(y: Int) = DurationYearsLocal(y)
        fun monthsLocal(m: Int) = DurationMonthsLocal(m)
    }
}

interface DayDuration : Duration {
    fun addDays(ts: Daystamp): Daystamp
    fun addAny (ts: Timestamp): Timestamp = when (ts) {
        is Daystamp -> addDays(ts)
        is TimestampFull -> addFull(ts)
    }
}

class DurationDaysLocal(private val d: Int) : DayDuration {
    override fun addFull(ts: TimestampFull) = TimestampFull(
            timeInMillis = addDay(ts.timeInMillis, ts.tz, d),
            tz = ts.tz)

    override fun addDays(ts: Daystamp) = Daystamp(daysSinceEpoch = ts.daysSinceEpoch + d)
}

class DurationMonthsLocal(private val m: Int) : DayDuration {
    override fun addFull(ts: TimestampFull) = TimestampFull(
            timeInMillis = addMonthToMillis(ts.timeInMillis, ts.tz, m),
            tz = ts.tz)

    override fun addDays(ts: Daystamp) = Daystamp (daysSinceEpoch = addMonthToDays(ts.daysSinceEpoch, m))
}

class DurationYearsLocal(private val y: Int) : DayDuration {
    override fun addFull(ts: TimestampFull) = TimestampFull(
            timeInMillis = addYearToMillis(ts.timeInMillis, ts.tz, y),
            tz = ts.tz)

    override fun addDays(ts: Daystamp) = Daystamp (daysSinceEpoch = addYearToDays(ts.daysSinceEpoch, y))
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

    fun dayMinute(d: Int, m: Int) = TimestampFull(
            timeInMillis = epochDayHourMinToMillis(tz, baseDays + d,
                    m / 60, m % 60), tz = tz)
    fun daySecond(d: Int, s: Int) = TimestampFull(
            timeInMillis = epochDayHourMinToMillis(tz, baseDays + d,
                    s / 3600, (s / 60) % 60) + (s%60) * SEC, tz = tz)
}

sealed class Timestamp: Parcelable {
    @Transient
    val ymd: YMD
        get () = getYMD(toDaystamp().daysSinceEpoch)
    @Transient
    val yd: YD
        get() = getYD(toDaystamp().daysSinceEpoch)

    abstract fun format(): FormattedString
    open operator fun plus(duration: DayDuration): Timestamp = duration.addAny(this)
    abstract fun toDaystamp(): Daystamp
    abstract fun obfuscateDelta(delta: Long): Timestamp

    fun isSameDay(other: Timestamp): Boolean = this.toDaystamp() == other.toDaystamp()
    abstract fun getMonth(): Month
    abstract fun getYear(): Int
}

@Parcelize
@Serializable
// Only date is known
data class Daystamp internal constructor(val daysSinceEpoch: Int): Timestamp(), Comparable<Daystamp> {
    override fun getMonth(): Month = getYMD(daysSinceEpoch).month

    override fun getYear(): Int = getYMD(daysSinceEpoch).year

    override fun toDaystamp(): Daystamp = this

    override fun compareTo(other: Daystamp): Int = daysSinceEpoch.compareTo(other.daysSinceEpoch)

    override fun obfuscateDelta(delta: Long) = Daystamp(daysSinceEpoch = daysSinceEpoch + ((delta + DAY/2) / DAY).toInt())

    override fun format(): FormattedString =
                TimestampFormatter.longDateFormat(this)
    fun adjust() : Daystamp = this
    fun promote(tz: MetroTimeZone, hour: Int, min: Int): TimestampFull = TimestampFull(
            tz = tz, timeInMillis = epochDayHourMinToMillis(tz, daysSinceEpoch, hour, min))

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
        val ymd = getYMD(daysSinceEpoch = daysSinceEpoch)
        return NumberUtils.zeroPad(ymd.year, 4) + "-" +
                NumberUtils.zeroPad(ymd.month.oneBasedIndex, 2) + "-" +
                NumberUtils.zeroPad(ymd.day, 2)
    }

    override fun toString(): String = isoDateFormat()

    /**
     * Represents a year, month and day in the Gregorian calendar.
     *
     * @param month Month, where January = 0.
     * @param day Day of the month, where the first day of the month = 1.
     */
    constructor(year: Int, month: Int, day: Int) : this(YMD(year, month, day))

    constructor(year: Int, month: Month, day: Int) : this(YMD(year, month, day))

    constructor(ymd: YMD) : this(
            daysSinceEpoch = ymd.daysSinceEpoch
    )

    constructor(yd: YD) : this(
            daysSinceEpoch = yd.daysSinceEpoch
    )
}

@Parcelize
@Serializable
// Precision or minutes and higher
data class TimestampFull internal constructor(val timeInMillis: Long,
                                            val tz: MetroTimeZone): Parcelable, Comparable<TimestampFull>, Timestamp() {
    override fun getMonth(): Month = toDaystamp().getMonth()

    override fun getYear(): Int = toDaystamp().getYear()

    override fun toDaystamp() = Daystamp(dhm.days)

    @Transient
    val dhm get() = getDaysFromMillis(timeInMillis, tz)

    override fun compareTo(other: TimestampFull): Int = timeInMillis.compareTo(other = other.timeInMillis)
    fun adjust() : TimestampFull =
            TripObfuscator.maybeObfuscateTS(if (Preferences.convertTimezone)
                TimestampFull(timeInMillis, MetroTimeZone.LOCAL) else this)

    operator fun plus(duration: Duration) = duration.addFull(this)
    override operator fun plus(duration: DayDuration) = duration.addFull(this)
    override fun format(): FormattedString = TimestampFormatter.dateTimeFormat(this)

    constructor(tz : MetroTimeZone, year: Int, month: Int, day: Int, hour: Int,
                min: Int, sec: Int = 0) : this(
            timeInMillis = epochDayHourMinToMillis(
                    tz, YMD(year, month, day).daysSinceEpoch, hour, min) + sec * SEC,
            tz = tz
    )

    constructor(tz : MetroTimeZone, year: Int, month: Month, day: Int, hour: Int,
                min: Int, sec: Int = 0) : this(
        year = year, month = month.zeroBasedIndex, day = day,
        hour = hour, min = min, sec = sec,
        tz = tz
    )

    constructor(tz: MetroTimeZone, dhm: DHM) : this(
            timeInMillis = epochDayHourMinToMillis(
                    tz, dhm.days, dhm.hour, dhm.min),
            tz = tz
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
        val daysSinceEpoch = timeInMillis / DAY
        val timeInDay = (timeInMillis % DAY) / MIN
        val ymd = getYMD(daysSinceEpoch = daysSinceEpoch.toInt())
        return NumberUtils.zeroPad(ymd.year, 4) + "-" +
                NumberUtils.zeroPad(ymd.month.oneBasedIndex, 2) + "-" +
                NumberUtils.zeroPad(ymd.day, 2) + " " +
                NumberUtils.zeroPad(timeInDay / 60, 2) + ":" +
                NumberUtils.zeroPad(timeInDay % 60, 2)
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
        val daysSinceEpoch = timeInMillis / DAY
        val sec = (timeInMillis % MIN) / SEC
        val timeInDay = (timeInMillis % DAY) / MIN
        val ymd = getYMD(daysSinceEpoch = daysSinceEpoch.toInt())
        return NumberUtils.zeroPad(ymd.year, 4) +
                NumberUtils.zeroPad(ymd.month.oneBasedIndex, 2) +
                NumberUtils.zeroPad(ymd.day, 2) + "-" +
                NumberUtils.zeroPad(timeInDay / 60, 2) +
                NumberUtils.zeroPad(timeInDay % 60, 2) +
                NumberUtils.zeroPad(sec, 2)
    }

    override fun toString(): String = isoDateTimeFormat() + "/$tz"

    override fun obfuscateDelta(delta: Long) = TimestampFull(timeInMillis = timeInMillis + delta, tz = tz)

    companion object {
        fun now() = makeNow()
    }
}

expect object TimestampFormatter {
    fun longDateFormat(ts: Timestamp): FormattedString
    fun dateTimeFormat(ts: TimestampFull): FormattedString
    fun timeFormat(ts: TimestampFull): FormattedString
}
