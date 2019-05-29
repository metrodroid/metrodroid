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
import au.id.micolous.metrodroid.util.ImmutableByteArray

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/BeijingMunicipal.java
@Parcelize
class BeijingTransitData (val validityStart: Int?,
                          val validityEnd: Int?,
                          override val serialNumber: String?,
                          override val trips: List<ChinaTrip>?,
                          val mBalance: Int?) : TransitData() {
    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_beijing)

    public override val balance: TransitBalance?
        get() = if (mBalance != null)
            TransitBalanceStored(TransitCurrency.CNY(mBalance),
                    null,
                    ChinaTransitData.parseHexDate(validityStart),
                    ChinaTransitData.parseHexDate(validityEnd))
        else
            null

    companion object {
        private const val FILE_INFO = 0x4

        private fun parse(card: ChinaCard): BeijingTransitData {
            val info = ChinaTransitData.getFile(card, FILE_INFO)?.binaryData

            return BeijingTransitData(serialNumber = parseSerial(card),
                    validityStart = info?.byteArrayToInt(0x18, 4),
                    validityEnd = info?.byteArrayToInt(0x1c, 4),
                    trips = ChinaTransitData.parseTrips(card) { data -> ChinaTrip(data) },
                    mBalance = ChinaTransitData.parseBalance(card)
            )
        }

        private val CARD_INFO = CardInfo(
                name = Localizer.localizeString(R.string.card_name_beijing),
                locationId = R.string.location_beijing,
                cardType = CardType.ISO7816,
                preview = true)

        val FACTORY: ChinaCardTransitFactory = object : ChinaCardTransitFactory {
            override val appNames: List<ImmutableByteArray>
                get() = listOf(
                        ImmutableByteArray.fromASCII("OC"),
                        ImmutableByteArray.fromASCII("PBOC")
                )

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: ChinaCard) =
                    TransitIdentity(Localizer.localizeString(R.string.card_name_beijing), parseSerial(card))

            override fun parseTransitData(card: ChinaCard) = parse(card)
        }

        private fun parseSerial(card: ChinaCard) = ChinaTransitData.getFile(card, FILE_INFO)?.
                binaryData?.getHexString(0, 8)
    }
}
