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

expect class StringResource
expect class PluralsResource
expect class DrawableResource

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

interface LocalizerInterface {
    fun localizeString(res: StringResource, vararg v: Any?): String
    fun localizeFormatted(res: StringResource, vararg v: Any?): FormattedString = FormattedString(localizeString(res, *v))
    fun localizeTts(res: StringResource, vararg v: Any?): FormattedString
    fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?): String
}

expect object Localizer : LocalizerInterface
