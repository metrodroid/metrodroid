/*
 * YarGorTransitFactory.kt
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

package au.id.micolous.metrodroid.transit.yargor

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.util.HashUtils

object YarGorTransitFactory : ClassicCardTransitFactory {
    val CARD_INFO = CardInfo(
            name = R.string.card_name_yargor,
            locationId = R.string.location_yaroslavl,
            imageId = R.drawable.yargor,
            imageAlphaId = R.drawable.iso7810_id1_alpha,
            cardType = CardType.MifareClassic,
            keysRequired = true, keyBundle = "yargor",
            region = TransitRegion.RUSSIA,
            preview = true)

    override val earlySectors: Int
        get() = 11

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun earlyCheck(sectors: List<ClassicSector>): Boolean = HashUtils.checkKeyHash(sectors[10],
            "yaroslavl", "0deaf06098f0f7ab47a7ea22945ee81a", "6775e7c1a73e0e9c98167a7665ef4bc1") >= 0

    override fun parseTransitIdentity(card: ClassicCard): TransitIdentity =
            TransitIdentity(R.string.card_name_yargor, YarGorTransitData.formatSerial(YarGorTransitData.getSerial(card)))

    override fun parseTransitData(card: ClassicCard) = YarGorTransitData.parse(card)
}
