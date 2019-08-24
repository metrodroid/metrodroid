/*
 * CardInfo.kt
 *
 * Copyright 2011 Eric Butler
 * Copyright 2015-2019 Michael Farrell
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
package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.multi.DrawableResource
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.StringResource

/**
 * List of all the cards we know about.
 */

class CardInfo(
        val name: String,
        val cardType: CardType,
        val locationId: StringResource? = null,
        val keysRequired: Boolean = false,
        val keyBundle: String? = null,

        /**
         * Indicates if the card is a "preview" / beta decoder, with possibly
         * incomplete / incorrect data.
         */
        val preview: Boolean = false,

        val resourceExtraNote: StringResource? = null,

        val imageId: DrawableResource? = null,
        val imageAlphaId: DrawableResource? = null,

        val iOSSupported: Boolean? = null) {

    // TODO: Make this the primary constructor
    constructor(
            name: StringResource,
            cardType: CardType,
            locationId: StringResource? = null,
            keysRequired: Boolean = false,
            keyBundle: String? = null,
            preview: Boolean = false,
            resourceExtraNote: StringResource? = null,
            imageId: DrawableResource? = null,
            imageAlphaId: DrawableResource? = null,
            iOSSupported: Boolean? = null
    ) : this(
            name = Localizer.localizeString(name),
            cardType = cardType,
            locationId = locationId,
            keysRequired = keysRequired,
            keyBundle = keyBundle,
            preview = preview,
            resourceExtraNote = resourceExtraNote,
            imageId = imageId,
            imageAlphaId = imageAlphaId,
            iOSSupported = iOSSupported)

    val hasBitmap get() = imageId != null
}
