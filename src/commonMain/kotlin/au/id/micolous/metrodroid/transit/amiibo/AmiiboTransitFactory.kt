/*
 * AmiiboTransitFactory.kt
 *
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.transit.amiibo

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.util.ImmutableByteArray

object AmiiboTransitFactory : UltralightCardTransitFactory {
    override fun check(card: UltralightCard) = (card.cardModel == "NTAG215" || card.pages.size == 136) &&
            // Amiibos are always locked and configured in the same way
            card.readPages(2,2).sliceOffLen(2, 6) == ImmutableByteArray.fromHex("0fe0f110ffee") &&
            (
                // https://github.com/metrodroid/metrodroid/issues/718
                card.getPage(0x4).data[0] == 0xa5.toByte() || // used
                card.getPage(0x4).data.isAllZero() // unused
            ) &&
            card.getPage(0x16).data[3] == 2.toByte() &&
            card.getPage(0x82).data.byteArrayToInt(0,3) == 0x01000f &&
            card.readPages(0x83,2) == ImmutableByteArray.fromHex("000000045f000000")

    override fun parseTransitData(card: UltralightCard) = AmiiboTransitData.parse(card)

    override fun parseTransitIdentity(card: UltralightCard) = TransitIdentity(AmiiboTransitData.NAME, null)

    private val CARD_INFO = CardInfo(
            name = AmiiboTransitData.NAME,
            cardType = CardType.MifareUltralight,
            locationId = R.string.location_worldwide,
            imageId = R.drawable.amiibo,
            region = TransitRegion.WORLDWIDE
    )

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)
}
