package au.id.micolous.metrodroid.util

import platform.Foundation.*

actual fun currencyNameBySymbol(symbol: String): String? =
    Preferences.currentLocale.localizedStringForCurrencyCode(symbol)

actual fun iso3166AlphaToName(isoAlpha: String): String? =
    Preferences.currentLocale.localizedStringForCountryCode(isoAlpha)
