/*
 * ErgTransaction.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Represents a transaction on an ERG MIFARE Classic card.
 */
@Parcelize
open class ErgTransaction(protected val purse: ErgPurseRecord,
                          protected val epoch: Int,
                          protected val currency: String,
                          protected val timezone: TimeZone) : Transaction() {


    // Implemented functionality.
    override fun getTimestamp(): Calendar {
        return convertTimestamp(epoch, timezone, purse.day, purse.minute)
    }

    override fun isTapOff(): Boolean {
        return false
    }

    override fun getFare(): TransitCurrency? {
        var o = purse.transactionValue
        if (purse.isCredit) {
            o *= -1
        }

        return TransitCurrency(o, currency)
    }

    override fun isSameTrip(other: Transaction): Boolean {
        return false
    }

    override fun isTapOn(): Boolean {
        return true
    }

    public override fun isTransfer(): Boolean {
        // TODO
        return false
    }

    companion object {
        private val EPOCH = GregorianCalendar(2000, Calendar.JANUARY, 1).timeInMillis

        private fun getEpoch(tz: TimeZone = Utils.UTC) : GregorianCalendar {
            val g = GregorianCalendar(tz)
            g.timeInMillis = EPOCH
            return g
        }


        fun convertTimestamp(epoch: Int, tz: TimeZone, day: Int = 0, minute: Int = 0): GregorianCalendar {
            val g = getEpoch(tz)
            g.add(Calendar.DATE, epoch + day)
            g.add(Calendar.MINUTE, minute)
            return g
        }
    }
}
