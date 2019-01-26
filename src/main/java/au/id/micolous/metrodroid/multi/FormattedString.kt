/*
 * FormattedString.kt
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

import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan

actual class FormattedString (val spanned: android.text.Spanned) {
    actual val unformatted: String
        get() = spanned.toString()
    actual constructor(input: String): this(SpannableString(input))

    actual override fun toString(): String = unformatted

    actual companion object {
        actual fun monospace(input: String): FormattedString {
            val res = SpannableString(input)
            res.setSpan(TypefaceSpan("monospace"), 0, input.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return FormattedString(res)
        }
    }
}