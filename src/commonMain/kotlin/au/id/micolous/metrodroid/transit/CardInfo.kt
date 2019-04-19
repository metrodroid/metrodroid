/*
 * CardInfo.java
 *
 * Copyright 2011 Eric Butler
 * Copyright 2015-2018 Michael Farrell
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
import au.id.micolous.metrodroid.multi.StringResource
import kotlin.jvm.JvmOverloads

/**
 * List of all the cards we know about.
 */

class CardInfo(val name: String,
               val cardType: CardType,
               val locationId: StringResource? = null,
               val keysRequired: Boolean = false,
               /**
                * Indicates if the card is a "preview" / beta decoder, with possibly
                * incomplete / incorrect data.
                *
                * @return true if this is a beta version of the card decoder.
                */
               val preview: Boolean = false,
               val resourceExtraNote: StringResource? = null,
               val imageId: DrawableResource? = null,
               val imageAlphaId: DrawableResource? = null) {

    val hasBitmap get() = imageAlphaId != null || imageId != null

    // For backwards compatibility. Remove once all the code is in kotlin.
    class Builder {
        private var mImageId: DrawableResource? = null
        private var mImageAlphaId: DrawableResource? = null
        private var mName: String? = null
        private var mLocationId: StringResource? = null
        private var mCardType: CardType? = null
        private var mKeysRequired: Boolean = false
        private var mPreview: Boolean = false
        private var mResourceExtraNote: StringResource? = null

        fun build(): CardInfo {
            return CardInfo(imageId = mImageId,
                    name = mName!!,
                    locationId = mLocationId,
                    cardType = mCardType!!,
                    keysRequired = mKeysRequired,
                    preview = mPreview,
                    resourceExtraNote = mResourceExtraNote,
                    imageAlphaId = mImageAlphaId)
        }

        @JvmOverloads
        fun setImageId(id: DrawableResource, alpha: DrawableResource? = null): Builder {
            mImageId = id
            mImageAlphaId = alpha
            return this
        }

        fun setName(name: String): Builder {
            mName = name
            return this
        }

        fun setLocation(id: StringResource): Builder {
            mLocationId = id
            return this
        }

        fun setCardType(type: CardType): Builder {
            mCardType = type
            return this
        }

        fun setKeysRequired(): Builder {
            mKeysRequired = true
            return this
        }

        fun setPreview(): Builder {
            mPreview = true
            return this
        }

        fun setExtraNote(id: StringResource): Builder {
            mResourceExtraNote = id
            return this
        }

    }

}
