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
package au.id.micolous.metrodroid.transit.nextfareul

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity

@Parcelize
class NextfareUnknownUltralightTransitData (override val capsule: NextfareUltralightTransitDataCapsule):
        NextfareUltralightTransitData() {

    override val timeZone: MetroTimeZone
        get() = TZ

    override val cardName: String
        get() = NAME

    override fun makeCurrency(value: Int) = TransitCurrency.XXX(value)

    private constructor(card: UltralightCard) : this(parse(card) {
        data, baseDate -> NextfareUnknownUltralightTransaction(data, baseDate)
    })

    override fun getProductName(productCode: Int): String? = null

    companion object {
        private const val NAME = "Nextfare Ultralight"

        internal val TZ = MetroTimeZone.UNKNOWN

        val FACTORY: UltralightCardTransitFactory = object : UltralightCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = emptyList()

            override fun check(card: UltralightCard): Boolean {
                val head = card.getPage(4).data.byteArrayToInt(0, 3)
                return head == 0x0a0400 || head == 0x0a0800
            }

            override fun parseTransitData(card: UltralightCard): TransitData {
                return NextfareUnknownUltralightTransitData(card)
            }

            override fun parseTransitIdentity(card: UltralightCard): TransitIdentity {
                return TransitIdentity(NAME, formatSerial(getSerial(card)))
            }
        }
    }
}
