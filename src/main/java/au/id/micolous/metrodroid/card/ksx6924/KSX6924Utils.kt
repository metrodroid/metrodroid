/*
 * KSX6924Utils.java
 *
 * Copyright 2018 Google
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.ksx6924

import au.id.micolous.metrodroid.util.NumberUtils.convertBCDtoInteger
import au.id.micolous.metrodroid.util.Utils
import java.util.*

object KSX6924Utils {
    const val INVALID_DATETIME = 0xffffffffffffffL
    const val INVALID_DATE = 0xffffffffL

    fun parseHexDateTime(value: Long, tz: TimeZone): Calendar? {
        if (value == INVALID_DATETIME)
            return null
        val g = GregorianCalendar(tz)
        g.set(convertBCDtoInteger((value shr 40).toInt()),
                convertBCDtoInteger((value shr 32 and 0xffL).toInt()) - 1,
                convertBCDtoInteger((value shr 24 and 0xffL).toInt()),
                convertBCDtoInteger((value shr 16 and 0xffL).toInt()),
                convertBCDtoInteger((value shr 8 and 0xffL).toInt()),
                convertBCDtoInteger((value and 0xffL).toInt()))
        return g
    }

    fun parseHexDate(value: Long): Calendar? {
        if (value >= INVALID_DATE)
            return null
        val g = GregorianCalendar(Utils.UTC)
        g.set(convertBCDtoInteger((value shr 16).toInt()),
                convertBCDtoInteger((value shr 8 and 0xffL).toInt()) - 1,
                convertBCDtoInteger((value and 0xffL).toInt()),
                0, 0, 0)

        return g
    }
}
