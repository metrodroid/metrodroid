/*
 * TransitRegion.kt
 *
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
package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.util.iso3166AlphaToName

sealed class TransitRegion {
    abstract val translatedName: String
    data class Iso (val code: String): TransitRegion () {
        override val translatedName: String
            get() = iso3166AlphaToName(code) ?: code
    }

    data class Custom(val res: StringResource): TransitRegion () {
        override val translatedName: String
            get() = Localizer.localizeString(res)
    }

    companion object {
        val XX = Iso("XX")
    }
}
