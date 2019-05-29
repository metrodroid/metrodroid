/*
 * TroikaTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem

/**
 * Troika cards.
 */

@Parcelize
class TroikaTransitData(private val mBlock4: TroikaBlock?,
                        private val mBlock7: TroikaBlock?,
                        private val mBlock8: TroikaBlock?) : Parcelable {

    val serialNumber: String?
        get() = (mBlock8 ?: mBlock7 ?: mBlock4)?.serialNumber

    val info: List<ListItem>?
        get() = (mBlock4?.info.orEmpty() + mBlock7?.info.orEmpty() + mBlock8?.info.orEmpty())
                .ifEmpty { null }

    val balance: TransitBalance
        get() = mBlock8?.balance ?: TransitCurrency.RUB(0)

    val warning: String?
        get() = if (mBlock8?.balance == null)
            Localizer.localizeString(R.string.troika_unformatted)
        else
            null

    val trips: List<Trip>
        get() = mBlock7?.trips.orEmpty() + mBlock8?.trips.orEmpty() + mBlock4?.trips.orEmpty()

    val subscriptions: List<Subscription>
        get() = listOfNotNull(
                mBlock4?.subscription,
                mBlock7?.subscription,
                mBlock8?.subscription)

    constructor(card: ClassicCard) : this(
            mBlock8 = decodeSector(card, 8),
            mBlock7 = decodeSector(card, 7),
            mBlock4 = decodeSector(card, 4)
    )

    companion object {
        internal val CARD_INFO = CardInfo(
                // seqgo_card_alpha has identical geometry
                imageId = R.drawable.troika_card,
                imageAlphaId = R.drawable.seqgo_card_alpha,
                name = Localizer.localizeString(R.string.card_name_troika),
                locationId = R.string.location_moscow,
                cardType = CardType.MifareClassic,
                resourceExtraNote = R.string.card_note_russia,
                keysRequired = true, preview = true)

        private const val TAG = "TroikaTransitData"

        private fun decodeSector(card: ClassicCard, idx: Int): TroikaBlock? {
            try {
                val sector = card.getSector(idx)
                if (sector is UnauthorizedClassicSector)
                    return null
                val block = sector.readBlocks(0, 3)
                return if (!TroikaBlock.check(block)) null else TroikaBlock.parseBlock(block)
            } catch (e: Exception) {
                Log.w(TAG, "Error decoding troika sector", e)
                return null
            }
        }
    }
}
