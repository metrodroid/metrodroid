/*
 * BlankClassicTransitData.java
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
package au.id.micolous.metrodroid.transit.unknown

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.InvalidClassicSector
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitIdentity

/**
 * Handle MIFARE Classic with no non-default data
 */
@Suppress("PLUGIN_WARNING")
@Parcelize
class BlankClassicTransitData : BlankTransitData() {
    override val cardName: String
        get() = Localizer.localizeString(R.string.blank_mfc_card)

    companion object {
        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            /**
             * @param card Card to read.
             * @return true if all sectors on the card are blank.
             */
            override fun check(card: ClassicCard): Boolean {
                val sectors = card.sectors
                var allZero = true
                var allFF = true
                // check to see if all sectors are blocked
                for ((secidx, s) in sectors.withIndex()) {
                    if (s is UnauthorizedClassicSector || s is InvalidClassicSector)
                        return false

                    val numBlocks = s.blocks.size

                    for ((blockidx, bl) in s.blocks.withIndex()) {
                        // Manufacturer data
                        if (secidx == 0 && blockidx == 0)
                            continue
                        if (blockidx == numBlocks - 1)
                            continue
                        if (!bl.data.isAllZero())
                            allZero = false
                        if (!bl.data.all { it == 0xff.toByte() })
                            allFF = false
                        if (!allZero && !allFF)
                            return false
                    }
                }
                return true
            }

            override fun parseTransitIdentity(card: ClassicCard) =
                    TransitIdentity(Localizer.localizeString(R.string.blank_mfc_card), null)

            override fun parseTransitData(card: ClassicCard) = BlankClassicTransitData()
        }
    }
}
