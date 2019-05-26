@file:JvmName("CountryKtActual")
package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.NumberUtils

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.CurrencyCode

actual fun getCurrencyDescriptorByCode(currencyCode: Int)
        : TransitCurrency.TransitCurrencyDesc {
    val currency = CurrencyCode.getByCode(currencyCode) ?: return TransitCurrency.TransitCurrencyDesc(
            currencyCode = "XXX",
            defaultDivisor = 100,
            name = null
    )
    return TransitCurrency.TransitCurrencyDesc(
            currency.currency.currencyCode,
            NumberUtils.pow(10, currency.currency.defaultFractionDigits).toInt(),
            name = currency.name
    )
}

actual fun countryCodeToName(countryCode: Int): String {
    val cc = if (countryCode > 0) {
        CountryCode.getByCode(countryCode)
    } else
        null
    if (cc != null) {
        return cc.toLocale().displayCountry
    } else {
        return Localizer.localizeString(R.string.unknown_format, countryCode)
    }
}
