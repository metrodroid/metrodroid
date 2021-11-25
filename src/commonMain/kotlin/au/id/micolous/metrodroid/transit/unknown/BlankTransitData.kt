/*
 * BlankTransitData.kt
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

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.transit.CardTransitFactory
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.TextListItem

/**
 * Base class for all types of cards that are blank.
 */
@Parcelize
class BlankTransitData(private val cardNameRes: StringResource): TransitData() {
    override val cardName: String
        get() = Localizer.localizeString(cardNameRes)

    override val serialNumber: String?
        get() = null

    override val info: List<ListItem>?
        get() = listOf(
                HeaderListItem(R.string.fully_blank_title, headingLevel = 1),
                TextListItem(R.string.fully_blank_desc)
        )
}

abstract class BlankTransitFactory<T> (private val name: StringResource) : CardTransitFactory<T> {
    override fun parseTransitIdentity(card: T) =
        TransitIdentity(Localizer.localizeString(name), null)

    override fun parseTransitData(card: T) = BlankTransitData(name)
}
