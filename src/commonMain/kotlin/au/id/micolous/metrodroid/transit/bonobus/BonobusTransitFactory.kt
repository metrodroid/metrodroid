/*
 * BonobusTransitFactory.kt
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

package au.id.micolous.metrodroid.transit.bonobus

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.HashUtils

object BonobusTransitFactory : ClassicCardTransitFactory {
    const val NAME = "Bonobús"

    private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_cadiz,
        cardType = CardType.MifareClassic,
        imageId = R.drawable.cadizcard,
        imageAlphaId = R.drawable.iso7810_id1_alpha,
        region = TransitRegion.SPAIN,
        keysRequired = true, keyBundle = "cadiz")

    override val allCards get() = listOf(CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            NAME, BonobusTransitData.getSerial(card).toString())

    override fun parseTransitData(card: ClassicCard): BonobusTransitData =
            BonobusTransitData.parse(card)

    override fun earlyCheck(sectors: List<ClassicSector>) =
            HashUtils.checkKeyHash(sectors[0], "cadiz",
                    // KeyB is readable and so doesn't act as a key
                    "cc2f0d405a4968f95100f776161929f6") >= 0

    override val earlySectors get() = 1
}
