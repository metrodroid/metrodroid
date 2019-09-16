/*
 * HSLUltralightTransitData.kt
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

/* Based on HSL documentation at https://github.com/HSLdevcom/hsl-card-java/blob/master/HSL%20Matkakortin%20kuvaus%20ja%20API%20kehitt%C3%A4jille%20v1.11.pdf */
package au.id.micolous.metrodroid.transit.hsl

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils.zeroPad

private const val NAME = "HSL Ultralight"

@Parcelize
data class HSLUltralightTransitData(override val trips: List<Trip>,
                                    override val subscriptions: List<Subscription>?,
                                    override val serialNumber: String,
                                    val applicationVersion: Int,
                                    val applicationKeyVersion: Int,
                                    val platformType: Int,
                                    val securityLevel: Int) : TransitData() {
    override val cardName get() = NAME

    override fun getRawFields(level: RawLevel): List<ListItem> = super.getRawFields(level).orEmpty() + listOf(
            ListItem("Application version", applicationVersion.toString()),
            ListItem("Application key version", applicationKeyVersion.toString()),
            ListItem("Platform type", platformType.toString()),
            ListItem("Security Level", securityLevel.toString())
    )
}

private fun getSerial(card: UltralightCard): String {
    val num = (card.tagId.byteArrayToInt(1, 3) xor card.tagId.byteArrayToInt(4, 3)) and 0x7fffff
    return card.readPages(4, 2).getHexString(1, 5) +
            zeroPad(num, 7) + card.pages[5].data.getBitsFromBuffer(16, 4)
}

private fun parse(card: UltralightCard): HSLUltralightTransitData {
    val raw = card.readPages(4, 12)
    //Read data from application info
    val version = raw.getBitsFromBuffer(0, 4)

    val arvo = HSLArvo.parseUL(raw.sliceOffLen(7, 41), version)
    return HSLUltralightTransitData(
            serialNumber = HSLTransitData.formatSerial(getSerial(card)),
            subscriptions = listOfNotNull(arvo),
            applicationVersion = version,
            applicationKeyVersion = card.pages[4].data.getBitsFromBuffer(4, 4),
            platformType = card.pages[5].data.getBitsFromBuffer(20, 3),
            securityLevel = card.pages[5].data.getBitsFromBuffer(23, 1),
            trips = TransactionTrip.merge(listOfNotNull(arvo?.lastTransaction)))
}

object HSLUltralightTransitFactory : UltralightCardTransitFactory {
    override fun check(card: UltralightCard): Boolean {
        val page4 = card.getPage(4).data
        return page4.getBitsFromBuffer(0, 4) in 1..2 &&
                page4.getBitsFromBuffer(8, 24) == 0x924621
    }

    override fun parseTransitData(card: UltralightCard) = parse(card)

    override fun parseTransitIdentity(card: UltralightCard) = TransitIdentity(NAME, HSLTransitData.formatSerial(getSerial(card)))
}
