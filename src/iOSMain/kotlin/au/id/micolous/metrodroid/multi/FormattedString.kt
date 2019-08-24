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

import au.id.micolous.metrodroid.util.Preferences
import platform.Foundation.*
import platform.UIKit.*

actual class FormattedString (val attributed: NSAttributedString): Parcelable {
    actual val unformatted: String
        get() = attributed.string()
    actual constructor(input: String): this(NSAttributedString.create(string = input))

    actual override fun toString(): String = unformatted

    actual operator fun plus(b: String): FormattedString = this + FormattedString(b)

    actual operator fun plus(b: FormattedString): FormattedString {
        val mut = NSMutableAttributedString()
        mut.appendAttributedString(attributed)
        mut.appendAttributedString(b.attributed)
        return FormattedString(mut)
    }

    actual fun substring(start: Int): FormattedString = FormattedString(attributed.attributedSubstringFromRange(NSMakeRange(start.toULong(), attributed.length - start.toULong())))
    actual fun substring(start: Int, end: Int): FormattedString = FormattedString(attributed.attributedSubstringFromRange(NSMakeRange(start.toULong(), end.toULong() - start.toULong())))

    actual companion object {
        private fun localeString(input: String, lang: String?, debugColor: UIColor): FormattedString {
            if (!Preferences.localisePlaces) {
                return FormattedString(input)
            }

            val attributes = HashMap<Any?, Any?>()
            attributes[UIAccessibilitySpeechAttributeLanguage] = lang ?: Preferences.language
            if (Preferences.debugSpans)
                attributes[NSForegroundColorAttributeName] = debugColor
            return FormattedString(NSAttributedString.create(string = input, attributes = attributes))
        }
        actual fun monospace(input: String): FormattedString = 
            FormattedString(NSAttributedString.create(string = input, attributes = mapOf<Any?, Any?>(NSFontAttributeName to UIFont.fontWithName("Courier", size=12.0))))
        actual fun language(input: String, lang: String): FormattedString = localeString(input, lang, UIColor.blueColor)
        actual fun english(input: String) = localeString(input, "en", UIColor.yellowColor)
        actual fun defaultLanguage(input: String) = localeString(input, null, UIColor.greenColor)
    }
}

actual class FormattedStringBuilder {
    actual fun append(value: StringBuilder): FormattedStringBuilder {
        append(value.toString())
        return this
    }
    actual fun append(value: String): FormattedStringBuilder {
        append(FormattedString(value))
        return this
    }
    actual fun append(value: FormattedString): FormattedStringBuilder {
        mut.appendAttributedString(value.attributed)
        return this
    }
    actual fun append(value: FormattedString, start: Int, end: Int): FormattedStringBuilder {
        mut.appendAttributedString(value.attributed.attributedSubstringFromRange(NSMakeRange(start.toULong(), end.toULong() - start.toULong())))
        return this
    }

    actual fun build(): FormattedString = FormattedString(mut)

    private val mut: NSMutableAttributedString

    actual constructor() {
        mut = NSMutableAttributedString()
    }
}
