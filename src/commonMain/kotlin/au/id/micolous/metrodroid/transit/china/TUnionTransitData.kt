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

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/TUnion.java
@Parcelize
class TUnionTransitData (override val serialNumber: String?,
                         private val mNegativeBalance: Int,
                         private val mBalance: Int,
                         override val trips: List<ChinaTrip>?,
                         val validityStart: Int?,
                         val validityEnd: Int?): TransitData() {

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_tunion)

    override val balance: TransitBalance
        get() = TransitBalanceStored(
                TransitCurrency.CNY(
                        if (mBalance > 0) mBalance else mBalance - mNegativeBalance
                ),
                null,
                ChinaTransitData.parseHexDate(validityStart), ChinaTransitData.parseHexDate(validityEnd))

    companion object {
        private val CARD_INFO = CardInfo(
                name = R.string.card_name_tunion,
                locationId = R.string.location_china_mainland,
                imageId = R.drawable.tunion,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816,
                region = TransitRegion.CHINA,
                preview = true)

        private fun parse(card: ChinaCard) : TUnionTransitData? {
            val file15 = ChinaTransitData.getFile(card, 0x15)?.binaryData ?: return null
            return TUnionTransitData(
                    serialNumber = parseSerial(card),
                    validityStart = file15.byteArrayToInt(20, 4),
                    validityEnd = file15.byteArrayToInt(24, 4),
                    trips = ChinaTransitData.parseTrips(card) { ChinaTrip(it) },
                    mBalance = card.getBalance(0)?.getBitsFromBuffer(1, 31) ?: 0,
                    mNegativeBalance = card.getBalance(1)?.getBitsFromBuffer(1, 31) ?: 0
            )
        }

        val FACTORY: ChinaCardTransitFactory = object : ChinaCardTransitFactory {
            override val appNames: List<ImmutableByteArray>
                get() = listOf(ImmutableByteArray.fromHex("A000000632010105"))

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: ChinaCard) =
                    TransitIdentity(Localizer.localizeString(R.string.card_name_tunion), parseSerial(card))

            override fun parseTransitData(card: ChinaCard) = parse(card)
        }

        private fun parseSerial(card: ChinaCard) =
                ChinaTransitData.getFile(card, 0x15)?.binaryData?.getHexString(10, 10)?.substring(1)
    }
}
