package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import platform.Foundation.*

actual fun currencyNameByCode(code: Int): String? {
        val symbol = ISO4217.getInfoByCode(code)?.symbol ?: return null
        return NSLocale.currentLocale.localizedStringForCurrencyCode(symbol)
}

actual fun countryCodeToName(countryCode: Int): String {
        val alpha = ISO3166.mapNumericToAlpha2(countryCode) ?: return Localizer.localizeString(R.string.unknown_format, countryCode)
        return NSLocale.currentLocale.localizedStringForCountryCode(alpha) ?: Localizer.localizeString(R.string.unknown_format, alpha)
}
