package au.id.micolous.metrodroid.util

import platform.Foundation.*

actual fun currencyNameBySymbol(symbol: String): String? =
    NSLocale.currentLocale.localizedStringForCurrencyCode(symbol)

actual fun iso3166AlphaToName(isoAlpha: String): String? =
    NSLocale.currentLocale.localizedStringForCountryCode(isoAlpha)
