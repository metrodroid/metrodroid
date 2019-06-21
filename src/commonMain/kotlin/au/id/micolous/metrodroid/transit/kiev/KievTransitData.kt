/*
 * KievTransitData.kt
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

package au.id.micolous.metrodroid.transit.kiev

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
class KievTransitData (private val mSerial: String,
                       override val trips: List<KievTrip>): TransitData() {

    override val serialNumber: String?
        get() = formatSerial(mSerial)

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_kiev)

    private constructor(card: ClassicCard) :this(
        mSerial = getSerial(card),
            trips = parseTrips(card))

    companion object {
        // It doesn't really have a name and is just called
        // "Ticket for Kiev Metro".
        private val CARD_INFO = CardInfo(
                name = Localizer.localizeString(R.string.card_name_kiev),
                locationId = R.string.location_kiev,
                imageId = R.drawable.kiev,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.MifareClassic,
                resourceExtraNote = R.string.card_note_kiev,
                keysRequired = true, preview = true, keyBundle = "kiev")

        private fun parseTrips(card: ClassicCard): List<KievTrip> =
                (0..5).map { card.getSector(3 + it / 3).getBlock(it % 3).data }
                        .filter { it.byteArrayToInt(0, 4) != 0 }.map { KievTrip(it) }

        private fun getSerial(card: ClassicCard): String =
                card.getSector(1).getBlock(0).data.sliceOffLen(6, 8)
                    .reverseBuffer().toHexString()

        private fun formatSerial(serial: String): String =
                NumberUtils.groupString(serial, " ", 4, 4, 4)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {

            override val earlySectors: Int
                get() = 2

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(sectors: List<ClassicSector>) =
                    HashUtils.checkKeyHash(sectors[1], "kiev",
                        "902a69a9d68afa1ddac7b61a512f7d4f") >= 0

            override fun parseTransitIdentity(card: ClassicCard) =
                    TransitIdentity(Localizer.localizeString(R.string.card_name_kiev), formatSerial(getSerial(card)))

            override fun parseTransitData(card: ClassicCard) = KievTransitData(card)
        }
    }
}
