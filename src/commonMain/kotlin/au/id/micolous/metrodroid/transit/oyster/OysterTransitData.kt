/*
 * OysterTransitData.kt
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
package au.id.micolous.metrodroid.transit.oyster

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * Oyster (Transport for London) on MIFARE Classic
 *
 * This is for old format cards that are **not** labelled with "D".
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/Oyster
 */
@Parcelize
data class OysterTransitData(
        private val mSerial: Int,
        override val balance: OysterPurse?,
        val transactions: List<OysterTransaction>,
        val refills: List<OysterRefill>,
        val passes: List<OysterTravelPass>
) : TransitData() {

    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    override val trips: List<Trip>?
        get() = TransactionTrip.merge(transactions) + refills

    override val subscriptions: List<Subscription>
        get() = passes

    override val onlineServicesPage: String?
        get() = "https://oyster.tfl.gov.uk/"

    private constructor(card: ClassicCard) : this(
            mSerial = getSerial(card),
            balance = OysterPurse.parse(card),
            transactions = OysterTransaction.parseAll(card).toList(),
            refills = OysterRefill.parseAll(card).toList(),
            passes = OysterTravelPass.parseAll(card).toList())

    companion object {
        private const val NAME = "Oyster"
        private val MAGIC_BLOCK_1 = ImmutableByteArray.fromHex("964142434445464748494A4B4C4D0101")

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_london,
                region = TransitRegion.UK,
                cardType = CardType.MifareClassic,
                resourceExtraNote = R.string.card_note_oyster,
                imageId = R.drawable.oyster_card,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                keysRequired = true,
                preview = true)

        private fun formatSerial(serial: Int) = NumberUtils.zeroPad(serial, 10)

        private fun getSerial(card: ClassicCard) = card[1, 0].data.byteArrayToIntReversed(1, 4)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME,
                    formatSerial(getSerial(card)))

            override fun parseTransitData(card: ClassicCard) = OysterTransitData(card)

            override fun earlyCheck(sectors: List<ClassicSector>) =
                sectors[0][1].data.contentEquals(MAGIC_BLOCK_1) and sectors[0][2].data.isAllFF()

            override val earlySectors get() = 1

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
