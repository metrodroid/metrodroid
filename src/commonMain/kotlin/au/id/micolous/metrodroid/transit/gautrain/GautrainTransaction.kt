/*
 * GautrainTransaction.kt
 *
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.transit.gautrain

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed
import au.id.micolous.metrodroid.transit.en1545.En1545Parser
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction
import au.id.micolous.metrodroid.transit.ovc.OVChipTransaction
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class GautrainTransaction(override val parsed: En1545Parsed) : En1545Transaction() {
    private val txnType: Int? get() = parsed.getInt(OVChipTransaction.TRANSACTION_TYPE)
    override val isTapOff: Boolean
        get() = txnType == 0x2a
    override val isTapOn: Boolean
        get() = txnType == 0x29
    override val lookup: En1545Lookup
        get() = GautrainLookup

    override val mode: Trip.Mode
        get() = when (txnType) {
            null -> Trip.Mode.TICKET_MACHINE
            0x29, 0x2a -> Trip.Mode.TRAIN
            else -> Trip.Mode.OTHER
        }

    companion object {
        fun parse(raw: ImmutableByteArray): GautrainTransaction? {
            if (raw.isAllZero())
                return null
            return GautrainTransaction(parsed = En1545Parser.parse(raw, OVChipTransaction.tripFields(reversed = true)))
        }
    }
}