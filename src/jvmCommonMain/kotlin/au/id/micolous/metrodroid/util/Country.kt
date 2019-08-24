@file:JvmName("CountryKtActual")
package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R

import java.util.*

actual fun currencyNameByCode(code: Int): String? {
    val symbol = ISO4217.getInfoByCode(code)?.symbol ?: return null
    val currency = Currency.getInstance(symbol) ?: return null
    return currency.displayName
}

private val specialLocales = mapOf(
    "CA" to Locale.CANADA,
    "CN" to Locale.CHINA,
    "DE" to Locale.GERMANY,
    "FR" to Locale.FRANCE,
    "GB" to Locale.UK,
    "IT" to Locale.ITALY,
    "JP" to Locale.JAPAN,
    "KR" to Locale.KOREA,
    "TW" to Locale.TAIWAN,
    "UK" to Locale.UK,
    "US" to Locale.US
)

actual fun countryCodeToName(countryCode: Int): String {
    val alpha = ISO3166.mapNumericToAlpha2(countryCode) ?: Localizer.localizeString(R.string.unknown_format, countryCode)
    val locale = specialLocales[alpha] ?: Locale("", alpha)
    return locale.displayCountry
}
