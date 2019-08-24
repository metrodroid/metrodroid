/*
 * TransitCurrency.kt
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

package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.FormattedString

import platform.Foundation.*
import platform.objc.*

internal actual fun formatCurrency(value: Int, divisor: Int, currencyCode: String, isBalance: Boolean): FormattedString {
    val prefix: String
    val modValue: Int
    if (!isBalance && value < 0) {
        prefix = "+ "
        modValue = -value
    } else {
        prefix= ""
        modValue = value
    }
    val decimal = NSDecimalNumber(modValue).decimalNumberByDividingBy(NSDecimalNumber(divisor))
    val localeInfo = mapOf<Any?, Any?>(
        NSLocaleCurrencyCode to currencyCode,
        NSLocaleLanguageCode to NSLocale.preferredLanguages[0])
    val locale = NSLocale(localeIdentifier=
        NSLocale.localeIdentifierFromComponents(localeInfo))
    val fmt = NSNumberFormatter()
    fmt.numberStyle = NSNumberFormatterCurrencyStyle
    fmt.locale = locale
    val formatted = fmt.stringFromNumber(decimal) ?: ("" + modValue.toDouble()/divisor.toDouble() + " " + currencyCode)
    return FormattedString(prefix + formatted)
}
