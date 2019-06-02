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
package au.id.micolous.metrodroid.transit.pisa

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.TransactionTrip
import au.id.micolous.metrodroid.transit.TransactionTripAbstract
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem

private const val NAME = "Pisa Ultralight"

@Parcelize
data class PisaUltralightTransitData(override val trips: List<TransactionTripAbstract>,
                                     private val mA: Int,
                                     private val mB: Long) : TransitData() {
    override val serialNumber: String? get() = null

    override val cardName get() = NAME

    override fun getRawFields(level: TransitData.RawLevel) = listOf(ListItem("A", mA.toString()),
            ListItem("B", mB.toString(16)))
}

private fun parse(card: UltralightCard): PisaUltralightTransitData {
    val trips = listOf(8, 12).mapNotNull { offset ->
        PisaTransaction.parseUltralight(card.readPages(offset, 4))
    }

    return PisaUltralightTransitData(
            mA = card.getPage(4).data[3].toInt() and 0xff,
            mB = card.readPages(6, 2).byteArrayToLong(),
            trips = TransactionTrip.merge(trips))
}

class PisaUltralightTransitFactory : UltralightCardTransitFactory {
    override fun check(card: UltralightCard) =
            card.getPage(4).data.byteArrayToInt(0, 3) == PisaTransitData.PISA_NETWORK_ID

    override fun parseTransitData(card: UltralightCard) = parse(card)

    override fun parseTransitIdentity(card: UltralightCard) = TransitIdentity(NAME, null)
}
