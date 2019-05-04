package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.transit.TransitCurrency

fun currencyNameByCode(code: Int): String? = getCurrencyDescriptorByCode(code).name

expect fun getCurrencyDescriptorByCode(currencyCode: Int)
        : TransitCurrency.TransitCurrencyDesc

expect fun countryCodeToName(countryCode: Int): String