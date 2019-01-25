/*
 * CEPASCard.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2013-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.cepascompat;

import au.id.micolous.metrodroid.card.CardProtocol
import au.id.micolous.metrodroid.transit.ezlinkcompat.EZLinkCompatTransitData

import kotlinx.serialization.Serializable

// This is only to read old dumps
@Serializable
data class CEPASCard(
        val purses: List<CEPASCompatPurse>,
        val histories: List<CEPASCompatHistory>,
        override val isPartialRead: Boolean
) : CardProtocol() {
    override fun parseTransitIdentity() = EZLinkCompatTransitData.parseTransitIdentity(this)

    override fun parseTransitData() = EZLinkCompatTransitData(this)

    fun getPurse(purse: Int) = purses.find { it.id == purse }

    fun getHistory(purse: Int) = histories.find { it.id == purse }
}
