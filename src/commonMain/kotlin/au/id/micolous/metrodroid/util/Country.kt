package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R

expect fun currencyNameBySymbol(symbol: String): String?

expect fun iso3166AlphaToName(isoAlpha: String): String?

fun currencyNameByCode(code: Int): String? {
    val symbol = ISO4217.getInfoByCode(code)?.symbol ?: return null
    return currencyNameBySymbol(symbol)
}

fun countryCodeToName(countryCode: Int): String {
    val alpha = ISO3166.mapNumericToAlpha2(countryCode) ?: Localizer.localizeString(R.string.unknown_format, countryCode)
    return iso3166AlphaToName(alpha) ?: Localizer.localizeString(R.string.unknown_format, alpha)
}
