/*
 * ErgTrip.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.erg

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord

/**
 * Represents a trip on an ERG MIFARE Classic card.
 */

@Parcelize
class ErgTripUnknown(override val purse: ErgPurseRecord,
                     override val epoch: Int): ErgTrip() {
    override val mode: Trip.Mode
        get() = Trip.Mode.OTHER

    override val currency: String
        get() = "XXX"

    override val tz: MetroTimeZone
        get() = MetroTimeZone.UNKNOWN
}

abstract class ErgTrip : Trip() {
    abstract val purse: ErgPurseRecord
    abstract val epoch: Int
    abstract val tz: MetroTimeZone
    abstract val currency: String

    // Implemented functionality.
    override val startTimestamp: Timestamp?
        get() = convertTimestamp(epoch, tz, purse.day, purse.minute)

    override val fare: TransitCurrency?
        get() {
            var o = purse.transactionValue
            if (purse.isCredit) {
                o *= -1
            }

            return TransitCurrency(o, currency)
        }

    companion object {
        private fun getEpoch(tz: MetroTimeZone): EpochLocal = Epoch.local(2000, tz)

        fun convertTimestamp(epoch: Int, tz: MetroTimeZone, day: Int, minute: Int): Timestamp =
                getEpoch(tz).dayMinute(day + epoch, minute)

        fun convertTimestamp(epoch: Int, tz: MetroTimeZone, day: Int) =
                getEpoch(tz).days(day + epoch)
    }
}
