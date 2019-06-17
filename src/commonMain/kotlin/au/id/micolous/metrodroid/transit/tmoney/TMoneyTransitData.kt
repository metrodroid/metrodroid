/*
 * TMoneyTransitData.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.tmoney

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Application
import au.id.micolous.metrodroid.card.ksx6924.KSX6924CardTransitFactory
import au.id.micolous.metrodroid.card.ksx6924.KSX6924PurseInfo
import au.id.micolous.metrodroid.card.ksx6924.KSX6924PurseInfoResolver
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem

@Parcelize
open class TMoneyTransitData internal constructor(
        protected val mBalance: Int,
        protected val mPurseInfo: KSX6924PurseInfo,
        private val mTrips: List<Trip>?) : TransitData() {

    override val serialNumber: String?
        get() = mPurseInfo.serial

    override val balance: TransitBalance?
        get() = mPurseInfo.buildTransitBalance(TransitCurrency.KRW(mBalance))

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_tmoney)

    override val info: List<ListItem>?
        get() = mPurseInfo.getInfo(purseInfoResolver)

    override val trips: List<Trip>?
        get() = mTrips

    constructor(card: KSX6924Application) : this(
            card.balance.byteArrayToInt(),
            card.purseInfo,
            card.transactionRecords?.mapNotNull {
                TMoneyTrip.parseTrip(it)
            }.orEmpty()
    )

    protected open val purseInfoResolver : KSX6924PurseInfoResolver
        get() = TMoneyPurseInfoResolver

    companion object {
        val CARD_INFO = CardInfo(
                imageId = R.drawable.tmoney_card,
                name = R.string.card_name_tmoney,
                locationId = R.string.location_seoul,
                cardType = CardType.ISO7816,
                preview = true)

        val FACTORY: KSX6924CardTransitFactory = object : KSX6924CardTransitFactory {
            override fun parseTransitIdentity(card: KSX6924Application) =
                    TransitIdentity(Localizer.localizeString(R.string.card_name_tmoney), card.serial)

            override fun parseTransitData(card: KSX6924Application) =
                    TMoneyTransitData(card)

            override val allCards = listOf(CARD_INFO)

            override fun check(card: KSX6924Application) = true
        }
    }
}
