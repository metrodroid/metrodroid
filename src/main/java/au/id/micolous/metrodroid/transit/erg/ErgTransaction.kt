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
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Represents a transaction on an ERG MIFARE Classic card.
 */
@Parcelize
open class ErgTransaction(protected val purse: ErgPurseRecord,
                          protected val epoch: GregorianCalendar?,
                          protected val currency: String) : Transaction() {

    // Implemented functionality.
    override fun getTimestamp(): Calendar {
        val ts = GregorianCalendar()
        ts.timeInMillis = epoch?.timeInMillis ?: 0
        ts.add(Calendar.DATE, purse.day)
        ts.add(Calendar.MINUTE, purse.minute)

        return ts
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

}
