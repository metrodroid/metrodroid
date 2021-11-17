package au.id.micolous.metrodroid.util

import java.util.*

actual fun currencyNameBySymbol(symbol: String): String? =
    Currency.getInstance(symbol)?.displayName

actual fun languageCodeToName(isoAlpha: String): String? =
    Locale.forLanguageTag(isoAlpha).displayName
