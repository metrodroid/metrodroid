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

fun FormattedString.equals(other: Any?): Boolean {
    return when (other) {
        null -> false
        is StringResource -> equals(Localizer.localizeString(other))
        is FormattedString -> equals(other.toString())
        is String -> toString() == other
        else -> false
    }
}

expect class FormattedString(input: String) : Parcelable {
    val unformatted: String

    override fun toString(): String

    operator fun plus(b: String): FormattedString
    operator fun plus(b: FormattedString): FormattedString

    fun substring(start: Int): FormattedString
    fun substring(start: Int, end: Int): FormattedString

    companion object {
        fun monospace(input: String): FormattedString
        fun defaultLanguage(input: String): FormattedString
        fun english(input: String): FormattedString
        fun language(input: String, lang: String): FormattedString
    }
}

expect class FormattedStringBuilder() {
    fun append(value: StringBuilder): FormattedStringBuilder
    fun append(value: String): FormattedStringBuilder
    fun append(value: FormattedString): FormattedStringBuilder
    fun append(value: FormattedString, start: Int, end: Int): FormattedStringBuilder
    fun build(): FormattedString
}

@Parcelize
class FormattedStringFallback (private val input: String): Parcelable {
    override fun toString(): String = unformatted
    val unformatted get() = input

    operator fun plus(b: String) = FormattedStringFallback(input + b)
    operator fun plus(b: FormattedStringFallback) = FormattedStringFallback(input + b.input)

    fun substring(start: Int) = FormattedStringFallback(input.substring(start))
    fun substring(start: Int, end: Int) = FormattedStringFallback(input.substring(start, end))

    companion object {
        fun monospace(input: String) = FormattedStringFallback(input)
        fun defaultLanguage(input: String) = FormattedStringFallback(input)
        fun english(input: String) = FormattedStringFallback(input)
        fun language(input: String, lang: String) = FormattedStringFallback(input)
    }
}

class FormattedStringBuilderFallback {
    fun append(value: StringBuilder): FormattedStringBuilderFallback {
        sb.append(value)
        return this
    }
    fun append(value: String): FormattedStringBuilderFallback {
        sb.append(value)
        return this
    }
    fun append(value: FormattedStringFallback): FormattedStringBuilderFallback {
        sb.append(value.unformatted)
        return this
    }
    fun append(value: FormattedStringFallback, start: Int, end: Int): FormattedStringBuilderFallback {
        sb.append(value.unformatted, start, end)
        return this
    }

    fun build(): FormattedStringFallback = FormattedStringFallback(sb.toString())

    private val sb: StringBuilder = StringBuilder()
}


// This is very basic but we have only formats of kind %s, %d, %x with possible positional argument
fun FormattedString.format(vararg args: Any?): FormattedString {
    val format = this
    if ('%' !in format.unformatted)
        return format
    var curSpec: StringBuilder? = null
    val res = FormattedStringBuilder()
    var argCtr = 0
    var lastProcessed = -1
    var specStart = -1
    for ((idx, curChar) in format.unformatted.withIndex()) {
        if (curSpec == null) {
            if (curChar == '%') {
                curSpec = StringBuilder("")
                specStart = idx
            }
            continue
        }
        if (curChar in "0123456789$") {
            curSpec.append(curChar)
            continue
        }
        if (curChar !in "sdx%") {
            // Invalid
            res.append(format, lastProcessed + 1, idx + 1)
            lastProcessed = idx
            curSpec = null
            continue
        }

        val cs = curSpec.toString()
        val argn = if ('$' in cs) cs.substringBefore('$').toInt() - 1 else argCtr
        argCtr++
        val arg = args.getOrNull(argn)
        res.append(format, lastProcessed + 1, specStart)
        lastProcessed = idx
        when (curChar) {
            's' -> { if (arg is FormattedString) res.append(arg) else res.append(arg.toString()) }
            'd' -> res.append((arg as? Number)?.toLong().toString())
            'x' -> res.append((arg as? Number)?.toLong()?.toString(16) ?: "null")
            '%' -> res.append("%")
            else -> res.append(format, specStart, idx + 1)
        }
    }
    res.append(format, lastProcessed + 1, format.unformatted.length)
    return res.build()
}
