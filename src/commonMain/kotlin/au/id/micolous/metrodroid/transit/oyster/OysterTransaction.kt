/*
 * OysterTransaction.kt
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
import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Transient

@Parcelize
class OysterTransaction(
        override val timestamp: Timestamp,
        // TODO: implement better
        private val sector: Int = 0,
        private val block: Int = 0,
        private val rawRecord: ImmutableByteArray = ImmutableByteArray.empty()
) : Transaction() {
    // TODO: implement
    @Transient
    override val isTapOff: Boolean
        get() = false
    // TODO: implement
    @Transient
    override val fare: TransitCurrency?
        get() = null
    // TODO: implement
    @Transient
    override val isTapOn: Boolean
        get() = true
    // TODO: implement
    override fun isSameTrip(other: Transaction) = false

    internal constructor(record: ImmutableByteArray, sector: Int, block: Int) : this(
            timestamp = OysterUtils.parseTimestamp(record, 6),
            sector = sector,
            block = block,
            rawRecord = record
    )

    override fun getRawFields(level: TransitData.RawLevel): String? {
        return when (level) {
            TransitData.RawLevel.ALL -> "s$sector/b$block: ${rawRecord.getHexString()}"
            else -> super.getRawFields(level)
        }
    }

    companion object {
        internal fun parseAll(card: ClassicCard) = sequence {
            for (sector in 9..13) {
                for (block in 0..2) {
                    // invalid
                    if (block == 0 && sector == 9) continue

                    try {
                        yield(OysterTransaction(card[sector, block].data, sector, block))
                    } catch (ex: Exception) {
                        Log.d("OysterTxn", "Parse error", ex)
                    }
                }
            }
        }
    }
}