/*
 * OysterRefill.kt
 *
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
package au.id.micolous.metrodroid.transit.oyster

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class OysterRefill(
        override val startTimestamp: Timestamp,
        private val amount: Int,
        // TODO: implement better
        private val rawRecord: ImmutableByteArray = ImmutableByteArray.empty()
) : Trip() {
    override val fare: TransitCurrency?
        get() = TransitCurrency.GBP(-amount)
    override val mode: Mode
        get() = Mode.TICKET_MACHINE

    internal constructor(record: ImmutableByteArray) : this(
            startTimestamp = OysterUtils.parseTimestamp(record),
            // estimate: £85 max top-up requires 14 bits
            amount = record.getBitsFromBufferLeBits(74, 14),
            rawRecord = record
    )

    override fun getRawFields(level: TransitData.RawLevel): String? {
        return when (level) {
            TransitData.RawLevel.ALL -> rawRecord.getHexString()
            else -> super.getRawFields(level)
        }
    }

    companion object {
        internal fun parseAll(card: ClassicCard) = sequence {
            for (sector in listOf(5)) {
                for (block in 0..2) {
                    try {
                        yield(OysterRefill(card[sector, block].data))
                    } catch (ex: Exception) {
                        Log.d("OysterRefill", "Parse error", ex)
                    }
                }
            }
        }
    }

}
