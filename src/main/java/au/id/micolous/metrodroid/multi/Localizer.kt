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
import android.os.Build
import androidx.annotation.RequiresApi
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LocaleSpan
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.util.Preferences
import androidx.annotation.VisibleForTesting
import java.util.*

import java.util.Locale

actual typealias StringResource = Int
actual typealias DrawableResource = Int
actual typealias PluralsResource = Int

actual object Localizer : LocalizerInterface {
    @set:VisibleForTesting
    var mock: LocalizerInterface? = null
    /**
     * Given a string resource (R.string), localize the string according to the language preferences
     * on the device.
     *
     * @param stringResource R.string to localize.
     * @param formatArgs     Formatting arguments to pass
     * @return Localized string
     */
    override fun localizeString(res: StringResource, vararg v: Any?): String {
        mock?.let { return it.localizeString(res, *v) }
        val appRes = MetrodroidApplication.instance.resources
        return appRes.getString(res, *v)
    }

    override fun localizeFormatted(res: StringResource, vararg v: Any?): FormattedString {
        mock?.let { return it.localizeFormatted(res, *v) }
        val appRes = MetrodroidApplication.instance.resources
        val spanned = SpannableString(appRes.getText(res))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Preferences.localisePlaces) {
            spanned.setSpan(LocaleSpan(Locale.getDefault()), 0, spanned.length, 0)
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
            mock?.let { return it.localizePlural(res, count, *v) }
            val appRes = MetrodroidApplication.instance.resources
            return appRes.getQuantityString(res, count, *v)
    }

    private val englishResources: Resources by lazy {
        val context = MetrodroidApplication.instance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            var conf = context.resources.configuration
            conf = Configuration(conf)
            conf.setLocale(Locale.ENGLISH)
            val localizedContext = context.createConfigurationContext(conf)
            localizedContext.resources
        } else {
            // Whatever, keep it translated as fallback
            context.resources
        }
    }

    fun englishString(res: StringResource, vararg v: Any?): String = englishResources.getString(res, *v)
}
