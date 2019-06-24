/*
 * MRTUltralightTransitData.kt
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

package au.id.micolous.metrodroid.transit.serialonly

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * MRT Ultralight cards.
 */

private const val NAME = "MRT single-use"

private val CARD_INFO = CardInfo(
        name = NAME,
        cardType = CardType.MifareUltralight,
        locationId = R.string.location_singapore,
        imageId = R.drawable.mrt_ul,
        imageAlphaId = R.drawable.mrt_ul_alpha,
        resourceExtraNote = R.string.card_note_card_number_only
)

private fun formatSerial(sn: Int) = "0001 ${NumberUtils.formatNumber(sn.toLong(), " ", 4, 4, 4)}"

private fun getSerial(card: UltralightCard) = card.getPage(15).data.byteArrayToInt()

@Parcelize
data class MRTUltralightTransitData(private val mSerial: Int) : SerialOnlyTransitData() {
    constructor(card: UltralightCard) : this(mSerial = getSerial(card))

    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    override val reason
        // The first 16 pages are readable but they don't change as the card is used.
        // Other pages are password-locked
        get() = Reason.LOCKED
}

class MRTUltralightTransitFactory : UltralightCardTransitFactory {
    override fun parseTransitIdentity(card: UltralightCard) = TransitIdentity(
            NAME, formatSerial(getSerial(card)))

    override fun parseTransitData(card: UltralightCard) = MRTUltralightTransitData(card)

    override fun check(card: UltralightCard) =
            card.getPage(3).data.byteArrayToInt() == 0x204f2400

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)
}

