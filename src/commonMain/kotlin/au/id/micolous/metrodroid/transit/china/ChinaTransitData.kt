/*
 * NewShenzhenTransitData.java
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

package au.id.micolous.metrodroid.transit.china

import au.id.micolous.metrodroid.card.iso7816.ISO7816File
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector
import au.id.micolous.metrodroid.card.china.ChinaCard
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

object ChinaTransitData {
    private val TZ = MetroTimeZone.BEIJING

    fun <T: ChinaTripAbstract>parseTrips(card: ChinaCard, createTrip: (ImmutableByteArray) -> T?): List<T> {
        val trips = mutableListOf<T>()
        val historyFile = getFile(card, 0x18)
        for (record in historyFile?.recordList.orEmpty()) {
            val t = createTrip(record)
            if (t == null || !t.isValid)
                continue
            trips.add(t)
        }
        return trips
    }

    // upper bit is some garbage
    fun parseBalance(card: ChinaCard): Int? = card.getBalance(0)?.getBitsFromBufferSigned(1, 31)

    fun getFile(card: ChinaCard, id: Int): ISO7816File? {
        val f = card.getFile(ISO7816Selector.makeSelector(0x1001, id))
        return f ?: card.getFile(ISO7816Selector.makeSelector(id))
    }

    fun parseHexDate(value: Int?): Timestamp? {
        if (value == null || value == 0)
            return null
        return Daystamp(
                NumberUtils.convertBCDtoInteger(value shr 16),
                NumberUtils.convertBCDtoInteger(value shr 8 and 0xff) - 1,
                NumberUtils.convertBCDtoInteger(value and 0xff))
    }

    fun parseHexDateTime(value: Long): Timestamp {
        return TimestampFull(TZ, NumberUtils.convertBCDtoInteger((value shr 40).toInt()),
                NumberUtils.convertBCDtoInteger((value shr 32 and 0xffL).toInt()) - 1,
                NumberUtils.convertBCDtoInteger((value shr 24 and 0xffL).toInt()),
                NumberUtils.convertBCDtoInteger((value shr 16 and 0xffL).toInt()),
                NumberUtils.convertBCDtoInteger((value shr 8 and 0xffL).toInt()),
                NumberUtils.convertBCDtoInteger((value and 0xffL).toInt()))
    }
}
