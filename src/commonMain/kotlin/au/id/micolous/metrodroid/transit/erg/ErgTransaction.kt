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

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.XXX
import au.id.micolous.metrodroid.transit.TransitCurrencyRef
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord

@Parcelize
class ErgUnknownTransaction(
        override val purse: ErgPurseRecord,
        override val epoch: Int) : ErgTransaction() {
    override val currency: TransitCurrencyRef get() = ::XXX
    override val timezone get() = MetroTimeZone.UNKNOWN
}

/**
 * Represents a transaction on an ERG MIFARE Classic card.
 */
abstract class ErgTransaction : Transaction() {
    abstract val purse: ErgPurseRecord
    abstract val epoch: Int
    protected abstract val currency: TransitCurrencyRef
    protected abstract val timezone: MetroTimeZone

    // Implemented functionality.
    override val timestamp get(): TimestampFull =
        convertTimestamp(epoch, timezone, purse.day, purse.minute)

    override val isTapOff get(): Boolean = false

    override val fare get(): TransitCurrency {
        var o = purse.transactionValue
        if (purse.isCredit) {
            o *= -1
        }

        return currency(o)
    }

    override fun isSameTrip(other: Transaction): Boolean {
        return false
    }

    override val isTapOn get(): Boolean {
        return true
    }

    override val isTransfer get(): Boolean {
        // TODO
        return false
    }

    override fun compareTo(other: Transaction): Int {
        // This prepares ordering for a later merge
        val ret = super.compareTo(other)

        return when {
            // Transactions are sorted by time alone -- but Erg transactions will have the same
            // timestamp for many things
            ret != 0 || other !is ErgTransaction -> ret

            // Put "top-ups" first
            purse.isCredit && purse.transactionValue != 0 -> -1
            other.purse.isCredit && other.purse.transactionValue != 0 -> 1

            // Then put "trips" first
            purse.isTrip -> -1
            other.purse.isTrip -> 1

            // Otherwise sort by value
            else -> purse.transactionValue.compareTo(other.purse.transactionValue)
        }
    }

    companion object {
        fun convertTimestamp(epoch: Int, tz: MetroTimeZone, day: Int = 0, minute: Int = 0): TimestampFull {
            val g = Epoch.utc(2000, tz)
            return g.dayMinute(epoch + day, minute)
        }
    }
}
