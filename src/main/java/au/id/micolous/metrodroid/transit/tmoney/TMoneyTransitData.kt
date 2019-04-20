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

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Application
import au.id.micolous.metrodroid.card.ksx6924.KSX6924CardTransitFactory
import au.id.micolous.metrodroid.card.ksx6924.KSX6924PurseInfo
import au.id.micolous.metrodroid.card.ksx6924.KSX6924PurseInfoResolver
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import kotlinx.android.parcel.Parcelize

@Parcelize
open class TMoneyTransitData internal constructor (
        protected open val mBalance: Int,
        protected open val purseInfo: KSX6924PurseInfo,
        private val mTrips: List<Trip>) : TransitData() {


    override val serialNumber: String?
        get() = purseInfo.serial

    override val balance: TransitBalance?
        get() = purseInfo.buildTransitBalance(TransitCurrency.KRW(mBalance))

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_tmoney)

    override val info: List<ListItem>?
        get() = purseInfo.getInfo(purseInfoResolver)

    override val trips: List<Trip>?
        get() = mTrips

    protected open val purseInfoResolver : KSX6924PurseInfoResolver
        get() = TMoneyPurseInfoResolver.INSTANCE

    constructor(tMoneyCard: KSX6924Application) : this(
            tMoneyCard.balance,
            tMoneyCard.purseInfo,
            tMoneyCard.transactionRecords?.mapNotNull {
                TMoneyTrip.parseTrip(it.data)
            }.orEmpty()
    )

    companion object {
        val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.tmoney_card)
                .setName(Localizer.localizeString(R.string.card_name_tmoney))
                .setLocation(R.string.location_seoul)
                .setCardType(CardType.ISO7816)
                .setPreview()
                .build()

        val FACTORY: KSX6924CardTransitFactory = object : KSX6924CardTransitFactory {
            override fun parseTransitIdentity(card: KSX6924Application) =
                    TransitIdentity(Localizer.localizeString(R.string.card_name_tmoney), card.serial)

            override fun parseTransitData(card: KSX6924Application) =
                    TMoneyTransitData(card)

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun check(card: KSX6924Application) = true
        }

        fun parseTransitIdentity(card: KSX6924Application): TransitIdentity {
            return TransitIdentity(Localizer.localizeString(R.string.card_name_tmoney), card.serial)
        }
    }
}
