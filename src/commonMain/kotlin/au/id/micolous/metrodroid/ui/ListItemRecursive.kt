/*
 * ListItemRecursive.kt
 *
 * Copyright 2018-2019 Google
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

@Parcelize
@Serializable
@SerialName("recursive")
data class ListItemRecursive constructor(
    override val text1: FormattedString,
    override val text2: FormattedString?,
    val subTree: List<ListItemInterface>?): ListItemInterface() {

    constructor(text1: String, text2: String?, subTree: List<ListItemInterface>?)
            : this(FormattedString(text1), text2?.let { FormattedString(it) }, subTree)

    constructor(text1Res: StringResource, text2: String?, subTree: List<ListItemInterface>?)
            : this(Localizer.localizeFormatted(text1Res),
        text2?.let { FormattedString(it) }, subTree)

    companion object {

        fun collapsedValue(name: String, value: FormattedString): ListItemInterface =
            collapsedValue(name, null, value)

        fun collapsedValue(nameRes: StringResource, value: FormattedString?): ListItemInterface =
            ListItemRecursive(nameRes, null,
                if (value != null) listOf(ListItem(null, value)) else null)

        fun collapsedValue(title: String, subtitle: String?, value: FormattedString?): ListItemInterface =
            ListItemRecursive(title, subtitle,
                    if (value != null) listOf(ListItem(null, value)) else null)
    }
}
