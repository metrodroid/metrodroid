/*
 * UnauthorizedTransitData.kt
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

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.TextListItem

/**
 * Base class for all types of cards where we are unable to read any useful data (without a key).
 */
abstract class UnauthorizedTransitData : TransitData() {
    override val serialNumber: String?
        get() = null

    open val isUnlockable: Boolean
        get() = false

    override val info: List<ListItem>?
        get() = listOf(
                HeaderListItem(R.string.fully_locked_title, headingLevel = 1),
                TextListItem(if (isUnlockable) R.string.fully_locked_desc_unlockable else R.string.fully_locked_desc)
        )
}
