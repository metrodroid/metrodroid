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

import au.id.micolous.metrodroid.MetrodroidApplication

actual typealias StringResource = Int
actual typealias DrawableResource = Int
actual typealias PluralsResource = Int

actual object Localizer {
    /**
     * Given a string resource (R.string), localize the string according to the language preferences
     * on the device.
     *
     * @param stringResource R.string to localize.
     * @param formatArgs     Formatting arguments to pass
     * @return Localized string
     */
    actual fun localizeString(res: StringResource, vararg v: Any?): String {
        val appRes = MetrodroidApplication.instance.resources
        return appRes.getString(res, *v)
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
    actual fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?): String {
            val appRes = MetrodroidApplication.instance.resources
            return appRes.getQuantityString(res, count, *v)
    }
}
