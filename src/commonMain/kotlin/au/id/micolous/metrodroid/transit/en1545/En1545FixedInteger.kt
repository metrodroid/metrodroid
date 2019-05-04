/*
 * En1545Fixed.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.en1545


import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

class En1545FixedInteger(private val mName: String, private val mLen: Int) : En1545Field {

    override fun parseField(b: ImmutableByteArray, off: Int, path: String, holder: En1545Parsed, bitParser: En1545Bits): Int {
        try {
            holder.insertInt(mName, path, bitParser(b, off, mLen))
        } catch (e: Exception) {
            Log.w(TAG, "Overflow when parsing en1545", e)
        }

        return off + mLen
    }

    companion object {
        private const val TAG = "En1545FixedInteger"

        fun dateName(base: String) = "${base}Date"

        fun timeName(base: String) = "${base}Time"

        fun timePacked16Name(base: String) = "${base}TimePacked16"

        fun timeLocalName(base: String) = "${base}TimeLocal"
        fun dateTimeName(base: String) = "${base}DateTime"

        fun dateTimeLocalName(base: String) = "${base}DateTimeLocal"

        private fun getEpoch(tz: MetroTimeZone) = Epoch.local(1997, tz)

        private fun utcEpoch(tz: MetroTimeZone) = Epoch.utc(1997, tz)

        private fun parseTime(epoch: EpochLocal, d: Int, t: Int): Timestamp? {
            return if (d == 0 && t == 0) null else epoch.dayMinute(d, t)
        }

        private fun parseTime(epoch: EpochUTC, d: Int, t: Int): Timestamp? {
            return if (d == 0 && t == 0) null else epoch.dayMinute(d, t)
        }

        private fun parseTimePacked16(epoch: EpochUTC, d: Int, t: Int): Timestamp? {
            return if (d == 0 && t == 0) null else epoch.dayHourMinuteSecond(d,
                    (t shr 11), (t shr 5 and 0x3f), (t and 0x1f) * 2)
        }

        fun parseTime(d: Int, t: Int, tz: MetroTimeZone) = parseTime(utcEpoch(tz), d, t)

        fun parseTimeLocal(d: Int, t: Int, tz: MetroTimeZone) = parseTime(getEpoch(tz), d, t)

        fun parseTimePacked16(d: Int, t: Int, tz: MetroTimeZone): Timestamp? {
            return parseTimePacked16(utcEpoch(tz), d, t)
        }

        fun parseDate(d: Int, tz: MetroTimeZone): Timestamp? {
            return if (d == 0) null else getEpoch(tz).days(d)
        }

        fun parseTimeSec(`val`: Int, tz: MetroTimeZone): Timestamp? {
            return if (`val` == 0) null else utcEpoch(tz).seconds(`val`.toLong())
        }

        fun parseTimeSecLocal(sec: Int, tz: MetroTimeZone): Timestamp? {
            return if (sec == 0) null else getEpoch(tz).daySecond(sec / 86400, sec % 86400)
        }

        fun parseBCDDate(date: Int): Timestamp {
            return Daystamp(NumberUtils.convertBCDtoInteger(date shr 16),
                    NumberUtils.convertBCDtoInteger((date shr 8) and 0xff) - 1,
                    NumberUtils.convertBCDtoInteger(date and 0xff))
        }

        fun date(name: String) = En1545FixedInteger(dateName(name), 14)
        fun time(name: String) = En1545FixedInteger(timeName(name), 11)
        fun timePacked16(name: String) = En1545FixedInteger(timePacked16Name(name), 16)
        fun BCDdate(name: String) = En1545FixedInteger(name, 32)
        fun dateTime(name: String) = En1545FixedInteger(dateTimeName(name), 30)
        fun dateTimeLocal(name: String) = En1545FixedInteger(dateTimeLocalName(name), 30)
        fun timeLocal(name: String) = En1545FixedInteger(timeLocalName(name), 11)
    }
}
