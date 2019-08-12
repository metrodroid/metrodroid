/*
 * BaseInstrumentedTestPlatform.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.test

import android.content.Context
import android.content.res.Resources
import android.os.Build

import java.util.*

object LocaleTools {
    private val LOCALES = mapOf(
        "en" to Locale.ENGLISH,
        "en-AU" to Locale("en", "AU"),
        "en-CA" to Locale.CANADA,
        "en-GB" to Locale.UK,
        "en-US" to Locale.US,
        "fr" to Locale.FRENCH,
        "fr-CA" to Locale.CANADA_FRENCH,
        "fr-FR" to Locale.FRANCE,
        "ja" to Locale.JAPANESE,
        "ja-JP" to Locale.JAPAN,
        "ru" to Locale("ru"),
        "ru-RU" to Locale("ru", "RU"),
        "zh-CN" to Locale.SIMPLIFIED_CHINESE,
        "zh-TW" to Locale.TRADITIONAL_CHINESE)

    fun compatLocaleForLanguageTag(languageTag: String): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Locale.forLanguageTag(languageTag)
        } else {
            LOCALES[languageTag]
            ?: throw IllegalArgumentException("For API < 21, add entry to LOCALES for: $languageTag")
        }
    }

    fun setResourcesLocale(l: Locale?, r: Resources) {
        val c = r.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            c.setLocale(l)
        } else {
            c.locale = l
        }
        r.updateConfiguration(c, r.displayMetrics)
    }

    fun setLocale(languageTag: String, r: Resources) {
        val l = compatLocaleForLanguageTag(languageTag)
        Locale.setDefault(l)
        setResourcesLocale(l, r)
    }
}
