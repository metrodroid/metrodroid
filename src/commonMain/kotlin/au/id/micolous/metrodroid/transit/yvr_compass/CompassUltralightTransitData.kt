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
package au.id.micolous.metrodroid.transit.yvr_compass

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.nextfareul.NextfareUltralightTransitData
import au.id.micolous.metrodroid.transit.nextfareul.NextfareUltralightTransitDataCapsule

/* Based on reference at http://www.lenrek.net/experiments/compass-tickets/. */
@Parcelize
class CompassUltralightTransitData (override val capsule: NextfareUltralightTransitDataCapsule)
    : NextfareUltralightTransitData() {

    override val timeZone: MetroTimeZone
        get() = TZ

    override val cardName: String
        get() = NAME

    override fun makeCurrency(value: Int) = TransitCurrency.CAD(value)

    private constructor(card: UltralightCard) : this(NextfareUltralightTransitData.parse(card) {
        raw, baseDate -> CompassUltralightTransaction(raw, baseDate) })

    override fun getProductName(productCode: Int): String? = productCodes[productCode]

    companion object {
        private const val NAME = "Compass"

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.yvr_compass_card,
                name = NAME,
                locationId = R.string.location_vancouver,
                cardType = CardType.MifareUltralight,
                resourceExtraNote = R.string.compass_note)

        internal val TZ = MetroTimeZone.VANCOUVER

        val FACTORY: UltralightCardTransitFactory = object : UltralightCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun check(card: UltralightCard): Boolean {
                val head = card.getPage(4).data.byteArrayToInt(0, 3)
                if (head != 0x0a0400 && head != 0x0a0800)
                    return false
                val page1 = card.getPage(5).data
                if (page1[1].toInt() != 1 || page1[2].toInt() and 0x80 != 0x80 || page1[3].toInt() != 0)
                    return false
                val page2 = card.getPage(6).data
                return page2.byteArrayToInt(0, 3) == 0
            }

            override fun parseTransitData(card: UltralightCard) =
                    CompassUltralightTransitData(card)

            override fun parseTransitIdentity(card: UltralightCard): TransitIdentity =
                    TransitIdentity(NAME, formatSerial(getSerial(card)))
        }

        private val productCodes = mapOf(
            // TODO: i18n
            0x01 to "DayPass",
            0x02 to "One Zone",
            0x03 to "Two Zone",
            0x04 to "Three Zone",
            0x0f to "Four Zone WCE (one way)",
            0x11 to "Free Sea Island",
            0x16 to "Exit",
            0x1e to "One Zone with YVR",
            0x1f to "Two Zone with YVR",
            0x20 to "Three Zone with YVR",
            0x21 to "DayPass with YVR",
            0x22 to "Bulk DayPass",
            0x23 to "Bulk One Zone",
            0x24 to "Bulk Two Zone",
            0x25 to "Bulk Three Zone",
            0x26 to "Bulk One Zone",
            0x27 to "Bulk Two Zone",
            0x28 to "Bulk Three Zone",
            0x29 to "GradPass"
        )
    }
}
