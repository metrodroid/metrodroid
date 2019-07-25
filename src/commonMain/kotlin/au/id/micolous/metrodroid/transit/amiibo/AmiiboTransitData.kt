/*
 * AmiiboTransitData.kt
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
package au.id.micolous.metrodroid.transit.amiibo

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.StationTableReader

private const val NAME = "Amiibo"

private val CARD_INFO = CardInfo(
        name = NAME,
        cardType = CardType.MifareUltralight,
        locationId = R.string.location_worldwide,
        imageId = R.drawable.amiibo
)

/**
 * Reader for Nintendo Amiibo tags.
 *
 * https://3dbrew.org/wiki/Amiibo#Data_structures
 */
@Parcelize
data class AmiiboTransitData internal constructor(
        private val character: Int,
        private val characterVariant: Int,
        private val figureType: Int,
        private val modelNumber: Int,
        private val series: Int
) : TransitData() {
    override val serialNumber: String? get() = null

    override val info: List<ListItem> get() = listOf(
            ListItem(R.string.amiibo_type, figureTypes[figureType]?.let { Localizer.localizeString(it) }
                    ?: Localizer.localizeString(R.string.unknown_format, figureType)),
            ListItem(R.string.amiibo_character, StationTableReader.getStation(AMIIBO_STR, character).stationName),
            ListItem(R.string.amiibo_character_variant, characterVariant.toString()),
            ListItem(R.string.amiibo_model_number, modelNumber.toString()),
            ListItem(R.string.amiibo_series, StationTableReader.getOperatorName(AMIIBO_STR, series, false))
    )
    override val cardName get() = NAME

    companion object {
        private const val AMIIBO_STR = "amiibo"
        private val figureTypes = mapOf(
                0 to R.string.amiibo_type_figure,
                1 to R.string.amiibo_type_card,
                2 to R.string.amiibo_type_yarn
        )
        internal fun parse(ultralightCard: UltralightCard): AmiiboTransitData = AmiiboTransitData(
                character = ultralightCard.getPage(0x15).data.byteArrayToInt(0, 2),
                characterVariant = ultralightCard.getPage(0x15).data.byteArrayToInt(2, 1),
                figureType = ultralightCard.getPage(0x15).data.byteArrayToInt(3, 1),
                modelNumber = ultralightCard.getPage(0x16).data.byteArrayToInt(0, 2),
                series = ultralightCard.getPage(0x16).data.getBitsFromBuffer(16, 8)
                )
    }
}

object AmiiboTransitFactory : UltralightCardTransitFactory {
    override fun check(card: UltralightCard) = (card.cardModel == "NTAG215" || card.pages.size == 136) &&
            // Amiibos are always locked and configured in the same way
            card.readPages(2,3).sliceOffLen(2, 7) == ImmutableByteArray.fromHex("0fe0f110ffeea5") &&
            card.getPage(0x16).data[3] == 2.toByte() &&
            card.getPage(0x82).data.byteArrayToInt(0,3) == 0x01000f &&
            card.readPages(0x83,2) == ImmutableByteArray.fromHex("000000045f000000")


    override fun parseTransitData(card: UltralightCard) = AmiiboTransitData.parse(card)

    override fun parseTransitIdentity(card: UltralightCard) = TransitIdentity(NAME, null)

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)
}

