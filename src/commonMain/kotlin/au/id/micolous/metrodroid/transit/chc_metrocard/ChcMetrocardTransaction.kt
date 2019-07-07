/*
 * ChcMetrocardTransaction.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.chc_metrocard

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.erg.ErgTransaction
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord
import au.id.micolous.metrodroid.util.StationTableReader

@Parcelize
class ChcMetrocardTransaction(
        override val purse: ErgPurseRecord,
        override val epoch: Int) : ErgTransaction() {
    override val currency get() = ChcMetrocardTransitData.CURRENCY
    override val timezone get() = ChcMetrocardTransitData.TIME_ZONE

    override fun getAgencyName(isShort: Boolean): String? {
        return StationTableReader.getOperatorName(
                ChcMetrocardTransitData.CHC_METROCARD_STR, purse.agency, isShort)
    }

    override val mode get(): Trip.Mode {
        val m = StationTableReader.getOperatorDefaultMode(
                ChcMetrocardTransitData.CHC_METROCARD_STR, purse.agency)

        return when (m) {
            // There is a historic tram that circles the city, but not a commuter service, and does
            // not accept Metrocard. Therefore, everything unknown is a bus.
            Trip.Mode.OTHER -> Trip.Mode.BUS

            else -> m
        }
    }

    override fun shouldBeMerged(other: Transaction): Boolean {
        return when {
            other !is ChcMetrocardTransaction -> return super.shouldBeMerged(other)

            // Don't merge things with different times
            timestamp.compareTo(other.timestamp) != 0 -> false

            // Don't merge in top-ups.
            purse.isCredit && purse.transactionValue != 0 -> false

            // Don't merge different agencyID
            purse.agency != other.purse.agency -> false

            // Merge whe one is a trip and the other is not a trip
            else -> purse.isTrip != other.purse.isTrip
        }
    }
}
