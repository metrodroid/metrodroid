/*
 * UriListItem.kt
 *
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.StringResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ListItem which supports directing to a website.
 */
@Parcelize
@Serializable
@SerialName("uri")
data class UriListItem(
    override val text1: FormattedString,
    override val text2: FormattedString, val uri: String)
    : ListItemInterface() {
    constructor(nameResource: StringResource, valueResource: StringResource, uri: String) :
            this(Localizer.localizeFormatted(nameResource),
            Localizer.localizeFormatted(valueResource), uri)
    constructor(nameResource: StringResource, value: FormattedString, uri: String) :
            this(Localizer.localizeFormatted(nameResource),
            value, uri)
}
