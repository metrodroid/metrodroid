@file:JvmName("LocalizerKtActual")
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

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.LocaleSpan
import android.text.style.TtsSpan
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.ui.HiddenSpan
import androidx.annotation.VisibleForTesting
import au.id.micolous.metrodroid.util.Utils
import java.util.*

import java.util.Locale

actual typealias StringResource = Int
actual typealias DrawableResource = Int
actual typealias PluralsResource = Int

actual object Localizer : LocalizerInterface {
    /**
     * Given a string resource (R.string), localize the string according to the language preferences
     * on the device.
     *
     * @param stringResource R.string to localize.
     * @param formatArgs     Formatting arguments to pass
     * @return Localized string
     */
    override fun localizeString(res: StringResource, vararg v: Any?): String {
        val appRes = MetrodroidApplication.instance.resources
        return appRes.getString(res, *v)
    }

    override fun localizeFormatted(res: StringResource, vararg v: Any?): FormattedString {
        val appRes = MetrodroidApplication.instance.resources
        val spanned = SpannableString(appRes.getText(res))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Preferences.localisePlaces) {
            spanned.setSpan(LocaleSpan(Locale.getDefault()), 0, spanned.length, 0)
            if (Preferences.debugSpans)
                spanned.setSpan(ForegroundColorSpan(Color.GREEN), 0, spanned.length, 0)
        }
        return FormattedString(spanned).format(*v)
    }
    /**
     * Given a plural resource (R.plurals), localize the string according to the language preferences
     * on the device.
     *
     * @param pluralResource R.plurals to localize.
     * @param quantity       Quantity to use for pluaralisation rules
     * @param formatArgs     Formatting arguments to pass
     * @return Localized string
     */
    override fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?): String {
            val appRes = MetrodroidApplication.instance.resources
            return appRes.getQuantityString(res, count, *v)
    }

    private val englishResources: Resources by lazy {
        val context = MetrodroidApplication.instance
        Utils.localeContext(context, Locale.ENGLISH).resources
    }

    fun englishString(res: StringResource, vararg v: Any?): String = englishResources.getString(res, *v)

    override fun localizeTts(res: StringResource, vararg v: Any?): FormattedString {
        val appRes = MetrodroidApplication.instance.resources
        val b = SpannableStringBuilder(appRes.getText(res))

        // Find the TTS-exclusive bits
        // They are wrapped in parentheses: ( )
        var x = 0
        while (x < b.toString().length) {
            val start = b.toString().indexOf("(", x)
            if (start == -1) break
            var end = b.toString().indexOf(")", start)
            if (end == -1) break

            // Delete those characters
            b.delete(end, end + 1)
            b.delete(start, start + 1)

            // We have a range, create a span for it
            b.setSpan(HiddenSpan(), start, --end, 0)

            x = end
        }

        // Find the display-exclusive bits.
        // They are wrapped in square brackets: [ ]
        x = 0
        while (x < b.toString().length) {
            val start = b.toString().indexOf("[", x)
            if (start == -1) break
            var end = b.toString().indexOf("]", start)
            if (end == -1) break

            // Delete those characters
            b.delete(end, end + 1)
            b.delete(start, start + 1)
            end--

            // We have a range, create a span for it
            // This only works properly on Lollipop. It's a pretty reasonable target for
            // compatibility, and most TTS software will not speak out Unicode arrows anyway.
            //
            // This works fine with Talkback, but *doesn't* work with Select to Speak.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                b.setSpan(TtsSpan.TextBuilder().setText(" ").build(), start, end, 0)
            }

            x = end
        }

        return FormattedString(b).format(*v)
    }
}
