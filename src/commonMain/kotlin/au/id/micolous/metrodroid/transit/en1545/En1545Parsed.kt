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

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.util.NumberUtils

import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

sealed class En1545Value : Parcelable

@Parcelize
class En1545ValueInt(val v: Int) : En1545Value()

@Parcelize
class En1545ValueString(val v: String) : En1545Value()

@Parcelize
class En1545Parsed(private val map: MutableMap<String, En1545Value> = mutableMapOf()) : Parcelable {

    fun insertInt(name: String, path: String, value: Int) {
        map[makeFullName(name, path)] = En1545ValueInt(value)
    }

    fun insertString(name: String, path: String, value: String) {
        map[makeFullName(name, path)] = En1545ValueString(value)
    }

    fun getInfo(skipSet: Set<String>): List<ListItem> = map.entries.sortedBy { it.key }
            .filter { (key, _) -> !skipSet.contains(getBaseName(key)) }.map { (key, l) ->
                when (l) {
                    is En1545ValueInt -> ListItem(key, NumberUtils.intToHex(l.v))
                    is En1545ValueString -> ListItem(key, l.v)
                }
            }

    private fun getBaseName(name: String): String {
        return name.substring(name.lastIndexOf('/') + 1)
    }

    private fun makeString(separator: String, skipSet: Set<String>): String {
        val ret = StringBuilder()
        for ((key, value) in map) {
            if (skipSet.contains(getBaseName(key)))
                continue
            ret.append(key).append(" = ")
            if (value is En1545ValueInt)
                ret.append(NumberUtils.intToHex(value.v))
            if (value is En1545ValueString)
                ret.append("\"").append(value.v).append("\"")
            ret.append(separator)
        }
        return ret.toString()
    }

    override fun toString(): String {
        return "[" + makeString(", ", emptySet()) + "]"
    }

    fun getInt(name: String, path: String = ""): Int? {
        return (map[makeFullName(name, path)] as? En1545ValueInt)?.v
    }

    fun getInt(name: String, vararg ipath: Int): Int? {
        val path = StringBuilder()
        for (iel in ipath)
            path.append("/").append(iel.toString())
        return (map[makeFullName(name, path.toString())] as? En1545ValueInt)?.v
    }

    fun getIntOrZero(name: String, path: String = "") = getInt(name, path) ?: 0

    fun getString(name: String, path: String = ""): String? {
        return (map[makeFullName(name, path)] as? En1545ValueString)?.v
    }

    fun getTimeStamp(name: String, tz: MetroTimeZone): Timestamp? {
        if (contains(En1545FixedInteger.dateTimeName(name)))
            return En1545FixedInteger.parseTimeSec(
                    getIntOrZero(En1545FixedInteger.dateTimeName(name)), tz)
        if (contains(En1545FixedInteger.dateTimeLocalName(name)))
            return En1545FixedInteger.parseTimeSecLocal(
                    getIntOrZero(En1545FixedInteger.dateTimeLocalName(name)), tz)
        if (contains(En1545FixedInteger.timeName(name)) && contains(En1545FixedInteger.dateName(name)))
            return En1545FixedInteger.parseTime(
                    getIntOrZero(En1545FixedInteger.dateName(name)),
                    getIntOrZero(En1545FixedInteger.timeName(name)), tz)
        if (contains(En1545FixedInteger.timeLocalName(name)) && contains(En1545FixedInteger.dateName(name)))
            return En1545FixedInteger.parseTimeLocal(
                    getIntOrZero(En1545FixedInteger.dateName(name)),
                    getIntOrZero(En1545FixedInteger.timeLocalName(name)), tz)
        if (contains(En1545FixedInteger.timePacked16Name(name)) && contains(En1545FixedInteger.dateName(name)))
            return En1545FixedInteger.parseTimePacked16(
                    getIntOrZero(En1545FixedInteger.dateName(name)),
                    getIntOrZero(En1545FixedInteger.timePacked16Name(name)), tz)
        return if (contains(En1545FixedInteger.dateName(name))) En1545FixedInteger.parseDate(
                getIntOrZero(En1545FixedInteger.dateName(name)), tz) else null
        // TODO: any need to support time-only cases?
    }

    fun getTimeStampString(name: String, tz: MetroTimeZone): String? {
        val timeFlag = getTimeStamp(name, tz)
        if (timeFlag == null) {
            val value = getInt(En1545FixedInteger.timeName(name)) ?: getInt(En1545FixedInteger.timeLocalName(name))
            ?: return null
            return dig2(value / 60) + ":" + dig2(value % 60)
        }
        return timeFlag.format().toString()
    }

    fun contains(name: String, path: String = ""): Boolean {
        return map.containsKey(makeFullName(name, path))
    }

    fun append(data: ImmutableByteArray, off: Int, field: En1545Field): En1545Parsed {
        field.parseField(data, off, "", this) { obj, offset, len -> obj.getBitsFromBuffer(offset, len) }
        return this
    }

    fun appendLeBits(data: ImmutableByteArray, off: Int, field: En1545Field): En1545Parsed {
        field.parseField(data, off, "", this) { obj, off, len -> obj.getBitsFromBufferLeBits(off, len) }
        return this
    }

    fun append(data: ImmutableByteArray, field: En1545Field): En1545Parsed {
        return append(data, 0, field)
    }

    fun appendLeBits(data: ImmutableByteArray, field: En1545Field): En1545Parsed {
        return appendLeBits(data, 0, field)
    }

    companion object {
        private fun makeFullName(name: String, path: String?): String {
            return if (path == null || path.isEmpty()) name else "$path/$name"
        }

        private fun dig2(v: Int): String = when (v) {
            in 0..9 -> "0$v"
            else -> "$v"
        }
    }
}
