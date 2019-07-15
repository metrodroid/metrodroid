/*
 * TransitIdentity.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

class TransitIdentity private constructor(
        private val nameString: String? = null,
        private val nameResource: StringResource? = null,
        /**
         * Optional serial number for the card, if known.
         */
        val serialNumber: String? = null) {

    /**
     * @param name Name of the card, as a string. Prefer to use the [StringResource] version instead.
     */
    constructor(name: String, serialNumber: String? = null) :
            this(nameString = name, serialNumber = serialNumber)

    /**
     * @param name Name of the card, as [StringResource].
     */
    constructor(name: StringResource, serialNumber: String? = null) :
            this(nameResource = name, serialNumber = serialNumber)

    /**
     * Gets the name for the card, localizing if appropriate.
     */
    val name: String get() {
        if (nameResource != null)
            return Localizer.localizeString(nameResource)

        if (nameString != null)
            return nameString

        // shouldn't happen
        return ""
    }
}
