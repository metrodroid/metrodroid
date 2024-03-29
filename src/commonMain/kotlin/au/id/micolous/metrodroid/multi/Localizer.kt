/*
 * Localizer.kt
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

package au.id.micolous.metrodroid.multi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer

expect class StringResource
expect class PluralsResource
expect class DrawableResource

expect object Rmap {
    val strings: Map<String, StringResource>
    val plurals: Map<String, PluralsResource>
    val drawables: Map<String, DrawableResource>
}

interface Rinterface {
    val string: Rstring
    val plurals: Rplurals
    val drawable: Rdrawable
}

val R get(): Rinterface = object: Rinterface {
    override val string get() = Rstring
    override val plurals get() = Rplurals
    override val drawable get() = Rdrawable
}

@OptIn(ExperimentalSerializationApi::class)
expect class StringResourceSerializer : KSerializer<StringResource>

internal fun stripTts(input: String): String {
    val b = StringBuilder()

    // Find the TTS-exclusive bits
    // They are wrapped in parentheses: ( )
    var x = 0
    while (x < input.length) {
        val start = input.indexOf("(", x)
        if (start == -1) break
        val end = input.indexOf(")", start)
        if (end == -1) break

        // Delete those characters
        b.append(input, x, start)
        x = end + 1
    }
    if (x < input.length)
        b.append(input, x, input.length)

    val c = StringBuilder()
    // Find the display-exclusive bits.
    // They are wrapped in square brackets: [ ]
    x = 0
    while (x < b.length) {
        val start = b.indexOf("[", x)
        if (start == -1) break
        val end = b.indexOf("]", start)
        if (end == -1) break
        c.append(b, x, start).append(b, start + 1, end)
        x = end + 1
    }
    if (x < b.length)
        c.append(b, x, b.length)

    return c.toString()
}

interface LocalizerInterfaceBase <T: String?, U: FormattedString?> {
    fun localizeString(res: StringResource, vararg v: Any?): T
    fun localizeFormatted(res: StringResource, vararg v: Any?): U
    fun localizeTts(res: StringResource, vararg v: Any?): U
    fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?): T
}

interface LocalizerInterfaceSkippable: LocalizerInterfaceBase<String?, FormattedString?> {
    override fun localizeFormatted(res: StringResource, vararg v: Any?): FormattedString? =
        localizeString(res, *v)?.let { FormattedString(it) }
}
interface LocalizerInterface: LocalizerInterfaceBase<String, FormattedString> {
    override fun localizeFormatted(res: StringResource, vararg v: Any?): FormattedString =
        FormattedString(localizeString(res, *v))
}

abstract class LocalizeFallbacker(
    var plist: List<LocalizerInterfaceSkippable>,
    val last: LocalizerInterface
): LocalizerInterface {
    override fun localizeString(res: StringResource, vararg v: Any?): String =
        plist.firstNotNullOfOrNull { it.localizeString(res, *v) } ?:
        last.localizeString(res, *v)

    override fun localizeTts(res: StringResource, vararg v: Any?): FormattedString =
        plist.firstNotNullOfOrNull { it.localizeTts(res, *v) } ?:
        last.localizeTts(res, *v)

    override fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?): String =
        plist.firstNotNullOfOrNull { it.localizePlural(res, count, *v) } ?:
        last.localizePlural(res, count, *v)

    override fun localizeFormatted(res: StringResource, vararg v: Any?): FormattedString =
        plist.firstNotNullOfOrNull { it.localizeFormatted(res, *v) } ?:
        last.localizeFormatted(res, *v)
}

expect object Localizer : LocalizerInterface
