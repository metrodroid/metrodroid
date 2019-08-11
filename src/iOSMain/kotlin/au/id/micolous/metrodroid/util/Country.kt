package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.transit.TransitCurrency

actual fun getCurrencyDescriptorByCode(currencyCode: Int) : TransitCurrency.TransitCurrencyDesc =
        TransitCurrency.TransitCurrencyDesc(
                currencyCode.toString(),
                100,
                name = "Currency <$currencyCode>"
        )

actual fun countryCodeToName(countryCode: Int): String = "Country <$countryCode>"