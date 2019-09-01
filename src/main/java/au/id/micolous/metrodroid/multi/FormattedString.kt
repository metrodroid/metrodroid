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

import android.graphics.Color
import au.id.micolous.metrodroid.util.Preferences
import android.os.Build
import android.os.Parcel
import android.text.*
import android.text.style.*
import au.id.micolous.metrodroid.ui.HiddenSpan
import java.util.Locale

actual class FormattedString (val spanned: android.text.Spanned): Parcelable {
    actual val unformatted: String
        get() = spanned.toString()
    actual constructor(input: String): this(SpannableString(input))
    constructor(input: CharSequence): this(input as? SpannableString ?: SpannableString(input))
    actual override fun toString(): String = unformatted

    actual operator fun plus(b: String): FormattedString = FormattedString(TextUtils.concat(spanned, b))

    actual operator fun plus(b: FormattedString): FormattedString = FormattedString(TextUtils.concat(spanned, b.spanned))

    actual fun substring(start: Int): FormattedString = FormattedString(spanned.subSequence(start, spanned.length))
    actual fun substring(start: Int, end: Int): FormattedString = FormattedString(spanned.subSequence(start, end))

    override fun equals(other: Any?) = (other is FormattedString) && (spanned == other.spanned)

    val spannableString: SpannableString
       get() = SpannableString(spanned)

    constructor(parcel: Parcel) : this(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel) as CharSequence)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        TextUtils.writeToParcel(spanned, parcel, flags);
    }

    override fun describeContents() = 0

    actual companion object {
        actual fun monospace(input: String): FormattedString {
            val res = SpannableString(input)
            res.setSpan(TypefaceSpan("monospace"), 0, input.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return FormattedString(res)
        }
        private fun localeString(input: String, lang: String?, debugColor: Int): FormattedString {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !Preferences.localisePlaces) {
                return FormattedString(input)
            }

            val locale = if (lang != null) Locale.forLanguageTag(lang) else Locale.getDefault()
            val localeSpan = LocaleSpan(locale)
            val res = SpannableString(input)
            res.setSpan(localeSpan, 0, input.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (Preferences.debugSpans)
                res.setSpan(ForegroundColorSpan(debugColor), 0, input.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return FormattedString(res)
        }
        actual fun language(input: String, lang: String): FormattedString = localeString(input, lang, Color.BLUE)
        actual fun english(input: String) = localeString(input, "en", Color.YELLOW)
        actual fun defaultLanguage(input: String) = localeString(input, null, Color.GREEN)

        @JvmField
        val CREATOR: android.os.Parcelable.Creator<FormattedString> = object: android.os.Parcelable.Creator<FormattedString> {
            override fun createFromParcel(parcel: Parcel) = FormattedString(parcel)

            override fun newArray(size: Int): Array<FormattedString?> = arrayOfNulls(size)
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
        ssb.append(duplicateSpans(value.spanned))
        return this
    }
    actual fun append(value: FormattedString, start: Int, end: Int): FormattedStringBuilder {
        ssb.append(duplicateSpans(value.spanned), start, end)
        return this
    }

    private fun duplicateSpan(span: Any): Any {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            when (span) {
                is TypefaceSpan -> return TypefaceSpan(span.typeface ?: return span)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when (span) {
                is LocaleSpan -> return LocaleSpan(span.locales)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            when (span) {
                is LocaleSpan -> return LocaleSpan(span.locale)
                is TtsSpan -> return TtsSpan(span.type, span.args)
            }
        }
        return when (span) {
            is ForegroundColorSpan -> ForegroundColorSpan(span.foregroundColor)
            is TypefaceSpan -> TypefaceSpan(span.family ?: return span)
            is StyleSpan -> StyleSpan(span.style)
            is HiddenSpan -> HiddenSpan()
            else -> span
        }
    }

    // Android interface doesn't allow the same object to be spanned
    // several times in the same string. So if we copy 2 different parts of
    // same input string only first copy preserves any common spans.
    // To overcome this we duplicate the spans before passing
    // string to SpannableStringBuilder.
    private fun duplicateSpans(input: Spanned): Spanned {
        val spans = input.getSpans(0, input.length, Any::class.java)
        if (spans.isEmpty())
            return input
        val result = SpannableString(input.toString())
        for (span in spans) {
            val copy = duplicateSpan(span)
            result.setSpan(copy, input.getSpanStart(span), input.getSpanEnd(span),
                    input.getSpanFlags(span))
        }
        return result
    }

    actual fun build(): FormattedString = FormattedString(ssb)

    private val ssb: SpannableStringBuilder

    actual constructor() {
        ssb = SpannableStringBuilder()
    }
}
