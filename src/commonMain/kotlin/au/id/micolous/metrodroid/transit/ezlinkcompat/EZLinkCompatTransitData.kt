/*
 * EZLinkTransitData.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
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


import au.id.micolous.metrodroid.card.cepascompat.CEPASCard
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData

// This is only to read old dumps
@Parcelize
class EZLinkCompatTransitData(override val serialNumber: String?,
                              private val mBalance: Int,
                              override val trips: List<EZLinkCompatTrip>?) : TransitData() {

    override val cardName: String
        get() = EZLinkTransitData.getCardIssuer(serialNumber)

    // This is stored in cents of SGD
    public override val balance: TransitCurrency?
        get() = TransitCurrency.SGD(mBalance)

    constructor(cepasCard: CEPASCard) : this(
            serialNumber = cepasCard.getPurse(3)?.can?.toHexString(),
            mBalance = cepasCard.getPurse(3)?.purseBalance ?: 0,
            trips = parseTrips(cepasCard))

    companion object {
        private fun parseTrips(card: CEPASCard): List<EZLinkCompatTrip>? {
            val cardName = EZLinkTransitData.getCardIssuer(
                    card.getPurse(3)?.can?.toHexString())
            val transactions = card.getHistory(3)?.transactions
            return transactions?.map { EZLinkCompatTrip(it, cardName) }
        }

        fun parseTransitIdentity(card: CEPASCard): TransitIdentity {
            val canNo = card.getPurse(3)?.can?.toHexString()
            return TransitIdentity(EZLinkTransitData.getCardIssuer(canNo), canNo)
        }
    }

}
