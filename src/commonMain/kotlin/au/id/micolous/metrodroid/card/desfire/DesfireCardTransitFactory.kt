/*
 * DesfireCardTransitFactory.kt
 *
 * Copyright 2011-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.desfire

import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.CardTransitFactory
import au.id.micolous.metrodroid.util.ImmutableByteArray

interface DesfireCardTransitFactory : CardTransitFactory<DesfireCard> {
    fun earlyCheck(appIds: IntArray): Boolean

    fun getCardInfo(appIds: IntArray): CardInfo? = allCards.firstOrNull()

    override fun check(card: DesfireCard): Boolean = earlyCheck(card.applications.keys.toIntArray())
    fun createUnlocker(appIds: Int, manufData: ImmutableByteArray): DesfireUnlocker? = null
}
