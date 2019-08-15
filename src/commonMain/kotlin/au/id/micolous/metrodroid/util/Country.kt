package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.transit.TransitCurrency

expect fun currencyNameByCode(code: Int): String?

expect fun getCurrencyDescriptorByCode(currencyCode: Int)
        : TransitCurrency.TransitCurrencyDesc

expect fun countryCodeToName(countryCode: Int): String