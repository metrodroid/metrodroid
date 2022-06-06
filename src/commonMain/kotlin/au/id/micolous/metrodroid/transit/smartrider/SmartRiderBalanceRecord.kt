/*
 * SmartRiderBalanceRecord.kt
 *
 * Copyright 2016-2022 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.smartrider

import au.id.micolous.metrodroid.card.classic.ClassicSector

class SmartRiderBalanceRecord(smartRiderType: SmartRiderType, sector: ClassicSector) {
    private val b = sector.allData
    val bitfield = SmartRiderTripBitfield(smartRiderType, b[2].toInt())

    val transactionNumber = b.byteArrayToIntReversed(3, 2)

    val firstTagOn = SmartRiderTagRecord.parseRecentTransaction(
        smartRiderType, b.sliceOffLen(5, 14)
    )
    val recentTagOn = SmartRiderTagRecord.parseRecentTransaction(
        smartRiderType, b.sliceOffLen(19, 14)
    )

    val totalFarePaid = b.byteArrayToIntReversed(33, 2)
    val defaultFare = b.byteArrayToIntReversed(35, 2)
    val remainingChargableFare = b.byteArrayToIntReversed(37, 2)
    val balance = b.byteArrayToIntReversed(39, 2) * if (bitfield.isBalanceNegative) {
        -1
    } else {
        1
    }

    override fun toString(): String {
        return "bitfield=[$bitfield], " +
            "transactionNumber=$transactionNumber, totalFarePaid=$totalFarePaid, " +
            "defaultFare=$defaultFare, remainingChargableFare=$remainingChargableFare, " +
            "balance=$balance,\n  trip1=[$firstTagOn]\n  trip2=[$recentTagOn]\n"
    }
}
