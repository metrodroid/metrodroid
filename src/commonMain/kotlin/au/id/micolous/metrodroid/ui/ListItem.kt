/*
 * ListItem.kt
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.ui

import au.id.micolous.metrodroid.multi.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@SerialName("normal")
data class ListItem(override val text1: FormattedString?,
               override val text2: FormattedString?): ListItemInterface() {

    constructor(nameResource: StringResource) : this(nameResource, null as FormattedString?)

    constructor(nameResource: StringResource, valueResource: StringResource) : this(nameResource, Localizer.localizeString(valueResource))

    constructor(nameResource: StringResource, value: String?) : this(Localizer.localizeString(nameResource), value)

    constructor(nameResource: StringResource, value: FormattedString?) : this(FormattedString(Localizer.localizeString(nameResource)), value)

    constructor(nameResource: StringResource, pluralsResource: PluralsResource, valueInt: Int) :
            this(nameResource, Localizer.localizePlural(pluralsResource, valueInt, valueInt))

    constructor(name: String) : this(FormattedString(name), null)

    constructor(name: String?, value: String?) : this(if (name != null) FormattedString(name) else null,
            if (value != null) FormattedString(value) else null)

    constructor(name: FormattedString) : this(name, null)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            is ListItem -> text1 == other.text1 && text2 == other.text2
            else -> false
        }
    }

    override fun hashCode(): Int {
        var result = text1?.hashCode() ?: 0
        result = 31 * result + (text2?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "$text1/$text2"
}
