/*
 * SerialOnlyTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 *
 * Authors: Vladimir Serbinenko, Michael Farrell
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

package au.id.micolous.metrodroid.transit.serialonly

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.UriListItem

abstract class SerialOnlyTransitData : TransitData() {
    protected open val extraInfo: List<ListItem>?
        get() = null

    protected abstract val reason: Reason
    final override val info get(): List<ListItem>? {
        val li = mutableListOf(
                ListItem(R.string.card_format, cardName),
                ListItem(R.string.card_serial_number, serialNumber))
        li += extraInfo ?: emptyList()
        li += ListItem(R.string.serial_only_card_header,
                when (reason) {
                    Reason.NOT_STORED -> R.string.serial_only_card_description_not_stored
                    Reason.LOCKED -> R.string.serial_only_card_description_locked
                    else -> R.string.serial_only_card_description_more_research
                }
        )
        moreInfoPage?.let {
            li += UriListItem(R.string.unknown_more_info, R.string.unknown_more_info_desc, it)
        }
        return li
    }

    protected enum class Reason {
        UNSPECIFIED,
        /** The card doesn't store the balance */
        NOT_STORED,
        /** The data we want is locked */
        LOCKED,
        /** More research about the card format is needed */
        MORE_RESEARCH_NEEDED
    }
}
