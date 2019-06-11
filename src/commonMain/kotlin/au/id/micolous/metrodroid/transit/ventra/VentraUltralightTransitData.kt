/*
 * TroikaUltralightTransitData.kt
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
package au.id.micolous.metrodroid.transit.ventra

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.nextfareul.NextfareUltralightTransitData
import au.id.micolous.metrodroid.transit.nextfareul.NextfareUltralightTransitDataCapsule

@Parcelize
class VentraUltralightTransitData (override val capsule: NextfareUltralightTransitDataCapsule):
        NextfareUltralightTransitData() {

    override val cardName: String
        get() = NAME

    override val timeZone: MetroTimeZone
        get() = TZ

    override fun makeCurrency(value: Int) = TransitCurrency.USD(value)

    override fun getProductName(productCode: Int): String? = null

    constructor(card: UltralightCard) : this(NextfareUltralightTransitData.parse(card)
    { raw, baseDate -> VentraUltralightTransaction(raw, baseDate) }
    )

    companion object {
        private val CARD_INFO = CardInfo(
                name = VentraUltralightTransitData.NAME,
                locationId = R.string.location_chicago,
                imageId = R.drawable.ventra,
                cardType = CardType.MifareUltralight,
                resourceExtraNote = R.string.compass_note)

        private const val NAME = "Ventra"

        internal val TZ = MetroTimeZone.CHICAGO

        val FACTORY: UltralightCardTransitFactory = object : UltralightCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun check(card: UltralightCard): Boolean {
                val head = card.getPage(4).data.byteArrayToInt(0, 3)
                if (head != 0x0a0400 && head != 0x0a0800)
                    return false
                val page1 = card.getPage(5).data
                if (page1[1].toInt() != 1 || page1[2].toInt() and 0x80 == 0x80 || page1[3].toInt() != 0)
                    return false
                val page2 = card.getPage(6).data
                return page2.byteArrayToInt(0, 3) == 0
            }

            override fun parseTransitData(card: UltralightCard) =
                    VentraUltralightTransitData(card)

            override fun parseTransitIdentity(card: UltralightCard) =
                    TransitIdentity(NAME,
                        NextfareUltralightTransitData.formatSerial(
                                NextfareUltralightTransitData.getSerial(card)))
        }
    }
}
