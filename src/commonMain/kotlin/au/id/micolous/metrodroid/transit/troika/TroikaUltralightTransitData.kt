/*
 * TroikaUltralightTransitData.java
 *
 * Copyright 2018 Google
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
package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.ui.ListItem

@Parcelize
class TroikaUltralightTransitData (private val mBlock: TroikaBlock): TransitData() {

    override val balances: List<TransitBalance>?
        get() = listOfNotNull(mBlock.balance)

    override val trips: List<Trip>?
        get() = mBlock.trips

    override val subscriptions: List<Subscription>?
        get() = listOfNotNull(mBlock.subscription)

    override val serialNumber: String?
        get() = mBlock.serialNumber

    override val cardName: String
        get() = mBlock.cardName

    override val info: List<ListItem>?
        get() = mBlock.info

    companion object {
        val FACTORY: UltralightCardTransitFactory = object : UltralightCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = emptyList()

            override fun check(card: UltralightCard): Boolean = TroikaBlock.check(card.getPage(4).data)

            override fun parseTransitIdentity(card: UltralightCard): TransitIdentity =
                    TroikaBlock.parseTransitIdentity(card.readPages(4, 2))

            override fun parseTransitData(card: UltralightCard) =
                    TroikaUltralightTransitData(TroikaBlock.parseBlock(card.readPages(4, 12)))
        }
    }
}
