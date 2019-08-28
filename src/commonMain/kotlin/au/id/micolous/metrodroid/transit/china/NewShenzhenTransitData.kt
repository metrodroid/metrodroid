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
import au.id.micolous.metrodroid.card.china.ChinaCardTransitFactory
import au.id.micolous.metrodroid.card.china.ChinaCard
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/ShenzhenTong.java
@Parcelize
class NewShenzhenTransitData (val validityStart: Int?,
                              val validityEnd: Int?,
                              private val mSerial: Int,
                              override val trips: List<NewShenzhenTrip>?,
                              val mBalance: Int?): TransitData() {

    override val serialNumber: String?
        get() = formatSerial(mSerial)

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_szt)

    public override val balance: TransitBalance?
        get() = if (mBalance != null)
            TransitBalanceStored(TransitCurrency.CNY(mBalance),
                    null,
                    ChinaTransitData.parseHexDate(validityStart),
                    ChinaTransitData.parseHexDate(validityEnd))
        else
            null

    companion object {
        private fun parse(card: ChinaCard) : NewShenzhenTransitData {
            val szttag = getTagInfo(card)

            return NewShenzhenTransitData(
                    validityStart = szttag?.byteArrayToInt(20, 4),
                    validityEnd = szttag?.byteArrayToInt(24, 4),
                    trips = ChinaTransitData.parseTrips(card)  { data -> NewShenzhenTrip(data) },
                    mSerial = parseSerial(card),
                    mBalance = ChinaTransitData.parseBalance(card)
            )
        }

        val CARD_INFO = CardInfo(
                imageId = R.drawable.szt_card,
                name = R.string.card_name_szt,
                locationId = R.string.location_shenzhen,
                cardType = CardType.FeliCa,
                region = TransitRegion.CHINA,
                preview = true)

        val FACTORY: ChinaCardTransitFactory = object : ChinaCardTransitFactory {
            override val appNames: List<ImmutableByteArray>
                get() = listOf(ImmutableByteArray.fromASCII("PAY.SZT"))

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: ChinaCard) =
                    TransitIdentity(Localizer.localizeString(R.string.card_name_szt), formatSerial(parseSerial(card)))

            override fun parseTransitData(card: ChinaCard) = parse(card)
        }

        private fun formatSerial(sn: Int): String {
            val digsum = NumberUtils.getDigitSum(sn.toLong())
            // Sum of digits must be divisible by 10
            val lastDigit = (10 - digsum % 10) % 10
            return "$sn($lastDigit)"
        }

        private fun getTagInfo(card: ChinaCard): ImmutableByteArray? {
            val file15 = ChinaTransitData.getFile(card, 0x15)
            if (file15 != null)
                return file15.binaryData
            val szttag = card.appProprietaryBerTlv ?: return null
            return ISO7816TLV.findBERTLV(szttag, "8c", false)
        }

        private fun parseSerial(card: ChinaCard) = getTagInfo(card)?.byteArrayToIntReversed(16, 4) ?: 0
    }
}
