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

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.transit.TransactionTrip
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import kotlinx.android.parcel.Parcelize

private const val NAME = "OVC Ultralight"

@Parcelize
data class OvcUltralightTransitData(private val mTrips: List<OVChipTransaction>) : TransitData() {
    override val serialNumber get() = null

    override val cardName get() = NAME

    override val trips get(): List<TransactionTrip> = TransactionTrip.merge(mTrips)
}

private fun parse(card: UltralightCard): OvcUltralightTransitData {
    val trips = listOf(4, 8).mapNotNull lam@{ offset ->
        OVChipTransaction.parseUltralight(
                card.readPages(offset, 4))
    }
    return OvcUltralightTransitData(mTrips = trips)
}

class OvcUltralightTransitFactory : UltralightCardTransitFactory {
    // getAllCards not implemented -- Classic already adds it to supported cards

    // FIXME: check with more samples
    override fun check(card: UltralightCard) =
        card.getPage(4).data[0] in listOf(0xc0.toByte(), 0xc8.toByte())

    override fun parseTransitData(ultralightCard: UltralightCard) = parse(ultralightCard)

    override fun parseTransitIdentity(card: UltralightCard) = TransitIdentity(NAME, null)
}
