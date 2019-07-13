@file:JvmName("FormattedStringActualKt")
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
import android.text.SpannableStringBuilder
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

actual class FormattedStringBuilder {
    actual fun append(value: StringBuilder): FormattedStringBuilder {
        ssb.append(value)
        return this
    }
    actual fun append(value: String): FormattedStringBuilder {
        ssb.append(value)
        return this
    }
    actual fun append(value: FormattedString): FormattedStringBuilder {
        ssb.append(value.spanned)
        return this
    }
    actual fun append(value: FormattedString, start: Int, end: Int): FormattedStringBuilder {
        ssb.append(value.spanned, start, end)
        return this
    }

    actual fun build(): FormattedString = FormattedString(ssb)

    private val ssb: SpannableStringBuilder

    actual constructor() {
        ssb = SpannableStringBuilder()
    }
}