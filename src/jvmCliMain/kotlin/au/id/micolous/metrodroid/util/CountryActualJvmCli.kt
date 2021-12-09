package au.id.micolous.metrodroid.util

import java.util.*

actual fun currencyNameBySymbol(symbol: String): String? =
    try {
	Currency.getInstance(symbol)?.displayName
    } catch (e: IllegalArgumentException) {
	null
    }


actual fun languageCodeToName(isoAlpha: String): String? =
    Locale.forLanguageTag(isoAlpha).displayName
