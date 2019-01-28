/*
 * CalypsoCardTransitFactory
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.card.calypso

import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.CardTransitFactory
import au.id.micolous.metrodroid.util.ImmutableByteArray

interface CalypsoCardTransitFactory : CardTransitFactory<CalypsoApplication> {
    override fun check(card: CalypsoApplication): Boolean {
        val tenv = card.ticketEnv ?: return false
        return check(tenv)
    }

    fun check(tenv: ImmutableByteArray): Boolean

    fun getCardInfo(tenv: ImmutableByteArray): CardInfo?
}
