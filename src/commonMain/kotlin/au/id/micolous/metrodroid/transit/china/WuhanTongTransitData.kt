/*
 * NewShenzhenTransitData.kt
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

package au.id.micolous.metrodroid.transit.china

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.china.ChinaCard
import au.id.micolous.metrodroid.card.china.ChinaCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.china.ChinaTransitData.getFile
import au.id.micolous.metrodroid.util.ImmutableByteArray

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/WuhanTong.java
@Parcelize
class WuhanTongTransitData(val validityStart: Int?,
                           val validityEnd: Int?,
                           override val serialNumber: String?,
                           override val trips: List<ChinaTrip>?,
                           val mBalance: Int?) : TransitData() {
    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_wuhantong)

    override val balance: TransitBalance?
        get() = if (mBalance != null)
            TransitBalanceStored(TransitCurrency.CNY(mBalance),
                    null,
                    ChinaTransitData.parseHexDate(validityStart),
                    ChinaTransitData.parseHexDate(validityEnd))
        else
            null

    companion object {
        private val CARD_INFO = CardInfo(
                name = R.string.card_name_wuhantong,
                locationId = R.string.location_wuhan,
                imageId = R.drawable.wuhantong,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816,
                preview = true)

        private fun parse(card: ChinaCard): WuhanTongTransitData {
            val file5 = ChinaTransitData.getFile(card, 0x5)?.binaryData
            return WuhanTongTransitData(serialNumber = parseSerial(card),
                    validityStart = file5?.byteArrayToInt(20, 4),
                    validityEnd = file5?.byteArrayToInt(16, 4),
                    trips = ChinaTransitData.parseTrips(card) { ChinaTrip(it) },
                    mBalance = ChinaTransitData.parseBalance(card)
            )
        }

        val FACTORY: ChinaCardTransitFactory = object : ChinaCardTransitFactory {
            override val appNames: List<ImmutableByteArray>
                get() = listOf(ImmutableByteArray.fromASCII("AP1.WHCTC"))

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: ChinaCard): TransitIdentity {
                return TransitIdentity(R.string.card_name_wuhantong, parseSerial(card))
            }

            override fun parseTransitData(card: ChinaCard): TransitData = parse(card)
        }

        private fun parseSerial(card: ChinaCard): String? = getFile(card, 0xa)?.binaryData?.getHexString(0, 5)
    }
}
