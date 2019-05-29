/*
 * UnauthorizedClassicTransitData.kt
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitIdentity

/**
 * Handle MIFARE Classic with no open sectors
 */
@Suppress("PLUGIN_WARNING")
@Parcelize
class UnauthorizedClassicTransitData private constructor() : UnauthorizedTransitData() {

    override val cardName: String
        get() = Localizer.localizeString(R.string.locked_mfc_card)

    override val isUnlockable: Boolean
        get() = true

    companion object {
        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            /**
             * This should be the last executed MIFARE Classic check, after all the other checks are done.
             *
             *
             * This is because it will catch others' cards.
             *
             * @param card Card to read.
             * @return true if all sectors on the card are locked.
             */
            // check to see if all sectors are blocked
            override fun check(card: ClassicCard) = card.sectors.all { it is UnauthorizedClassicSector }

            override fun parseTransitIdentity(card: ClassicCard) =
                    TransitIdentity(Localizer.localizeString(R.string.locked_mfc_card), null)

            override fun parseTransitData(card: ClassicCard) =
                    UnauthorizedClassicTransitData()
        }
    }
}
