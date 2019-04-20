/*
 * SnapperTransitData.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.snapper

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Application
import au.id.micolous.metrodroid.card.ksx6924.KSX6924CardTransitFactory
import au.id.micolous.metrodroid.card.ksx6924.KSX6924PurseInfo
import au.id.micolous.metrodroid.card.ksx6924.KSX6924PurseInfoResolver
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData
import kotlinx.android.parcel.Parcelize

@Parcelize
class SnapperTransitData internal constructor (
        override val mBalance: Int,
        override val purseInfo: KSX6924PurseInfo,
        val mTrips: List<Trip>) : TMoneyTransitData(mBalance, purseInfo, mTrips) {

    override val balance: TransitBalance?
        get() = purseInfo.buildTransitBalance(TransitCurrency.NZD(mBalance))

    override val cardName: String
        get() = NAME

    // TODO: Handle Snapper codes properly
    override val purseInfoResolver: KSX6924PurseInfoResolver
        get() = KSX6924PurseInfoResolver.INSTANCE

    constructor(tMoneyCard: KSX6924Application) : this (
            tMoneyCard.balance,
            tMoneyCard.purseInfo,
            TransactionTrip.merge(getSnapperTransactionRecords(tMoneyCard).map {
                SnapperTransaction.parseTransaction(it.first.data, it.second.data) })

    )

    companion object {
        private const val NAME = "Snapper"

        val CARD_INFO = CardInfo.Builder()
                .setName(NAME)
                .setLocation(R.string.location_wellington_nz)
                .setCardType(CardType.ISO7816)
                .setPreview()
                .build()

        val FACTORY: KSX6924CardTransitFactory = object : KSX6924CardTransitFactory {
            override fun parseTransitIdentity(card: KSX6924Application) =
                    TransitIdentity(NAME, card.serial)

            override fun parseTransitData(card: KSX6924Application) = SnapperTransitData(card)

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            // Snapper cards have a slightly different record format: some bytes are always FF.
            override fun check(card: KSX6924Application) = card.getSfiFile(4)?.records?.all {
                    it.data.sliceArray(26 until 46).isAllFF()
                } ?: false
        }

        private fun getSnapperTransactionRecords(tMoneyCard: KSX6924Application): List<Pair<ISO7816Record, ISO7816Record>> {
            val trips = tMoneyCard.getSfiFile(3) ?: return emptyList()
            val balances = tMoneyCard.getSfiFile(4) ?: return emptyList()

            return trips.records zip balances.records
        }
    }
}