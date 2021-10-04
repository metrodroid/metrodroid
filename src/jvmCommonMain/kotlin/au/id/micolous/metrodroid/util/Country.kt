@file:JvmName("CountryKtActual")
package au.id.micolous.metrodroid.util

import java.util.*

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

actual fun iso3166AlphaToName(isoAlpha: String): String? {
    val locale = specialLocales[isoAlpha] ?: Locale("", isoAlpha)
    return locale.displayCountry
}
