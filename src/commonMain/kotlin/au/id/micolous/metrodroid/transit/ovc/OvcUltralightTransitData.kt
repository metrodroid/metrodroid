/*
 * OvcUltralightTransitData.kt
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
package au.id.micolous.metrodroid.transit.ovc

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransactionTrip
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion

private const val NAME = "OV-chipkaart (single-use)"
private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_the_netherlands,
        imageId = R.drawable.ovchip_single_card,
        imageAlphaId = R.drawable.iso7810_id1_alpha,
        region = TransitRegion.NETHERLANDS,
        cardType = CardType.MifareUltralight)

@Parcelize
data class OvcUltralightTransitData(private val mTrips: List<OVChipTransaction>) : TransitData() {
    override val serialNumber: String? get() = null

    override val cardName get() = NAME

    override val trips get() = TransactionTrip.merge(mTrips)
}

private fun parse(card: UltralightCard): OvcUltralightTransitData {
    val trips = listOf(4, 8).mapNotNull lam@{ offset ->
        OVChipTransaction.parseUltralight(
                card.readPages(offset, 4))
    }
    return OvcUltralightTransitData(mTrips = trips)
}

class OvcUltralightTransitFactory : UltralightCardTransitFactory {
    // These are listed twice, because unlike regular OVC, single use cards don't need keys, and
    // they are supported on more devices than regular OVC (MFC).
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    // FIXME: check with more samples
    override fun check(card: UltralightCard) =
        card.getPage(4).data[0] in listOf(0xc0.toByte(), 0xc8.toByte())

    override fun parseTransitData(card: UltralightCard) = parse(card)

    override fun parseTransitIdentity(card: UltralightCard) = TransitIdentity(NAME, null)
}
