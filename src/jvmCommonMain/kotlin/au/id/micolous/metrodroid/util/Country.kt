@file:JvmName("CountryKtActual")
package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.NumberUtils

import com.neovisionaries.i18n.CurrencyCode
import java.util.*

actual fun currencyNameByCode(code: Int): String? {
    val symbol = getCurrencyDescriptorByCode(code).currencyCode
    if (symbol == "XXX" && code != 999)
        return null
    val currency = Currency.getInstance(symbol) ?: return null
    return currency.displayName
}

actual fun getCurrencyDescriptorByCode(currencyCode: Int)
        : TransitCurrency.TransitCurrencyDesc {
    val currency = CurrencyCode.getByCode(currencyCode) ?: return TransitCurrency.TransitCurrencyDesc(
            currencyCode = "XXX",
            defaultDivisor = 100
    )
    return TransitCurrency.TransitCurrencyDesc(
            currency.currency.currencyCode,
            NumberUtils.pow(10, currency.currency.defaultFractionDigits).toInt()
    )
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
