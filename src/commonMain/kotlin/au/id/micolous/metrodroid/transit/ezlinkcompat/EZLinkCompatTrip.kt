/*
 * EZLinkTrip.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.ezlinkcompat

import au.id.micolous.metrodroid.card.cepascompat.CEPASCompatTransaction
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.ezlink.CEPASTransaction
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTrip
import au.id.micolous.metrodroid.transit.ezlink.EZUserData

@Parcelize
class EZLinkCompatTrip (private val mTransaction: CEPASCompatTransaction,
                        private val mCardName: String): Trip() {

    override val startTimestamp: Timestamp?
        get() = EZLinkTransitData.timestampToCalendar(
                mTransaction.unixDate - 788947200 + 16 * 3600)

    override val routeName: FormattedString?
        get() = EZUserData.parse(mTransaction.userData, type).routeName

    override val fare: TransitCurrency?
        get() = if (type === CEPASTransaction.TransactionType.CREATION) null else TransitCurrency.SGD(-mTransaction.amount)

    private val type: CEPASTransaction.TransactionType
        get() = CEPASTransaction.getType(mTransaction.type)

    override val startStation: Station?
        get() = EZUserData.parse(mTransaction.userData, type).startStation

    override val endStation: Station?
        get() = EZUserData.parse(mTransaction.userData, type).endStation

    override val mode: Trip.Mode
        get() = EZLinkTrip.getMode(type)

    override fun getAgencyName(isShort: Boolean) =
            EZLinkTrip.getAgencyName(type, mCardName, isShort)
}
