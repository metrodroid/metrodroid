package au.id.micolous.metrodroid.util

import android.os.Build
import java.util.*

actual fun currencyNameBySymbol(symbol: String): String? =
    try {
        Currency.getInstance(symbol)?.let {
            // SDK >= 24 doesn't throw IllegalArgumentException for unknown currency codes, despite
            // the documentation (still) saying it should:
            // https://developer.android.com/reference/java/util/Currency#getInstance(java.lang.String)
            //
            // Thankfully its lies are easily exposed: unknown currencies have numericCode == 0.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && it.numericCode == 0) {
                null
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                it.displayName
            } else {
                it.currencyCode
            }
        }
    } catch (e: IllegalArgumentException) {
        null
    }

actual fun languageCodeToName(isoAlpha: String): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Locale.forLanguageTag(isoAlpha)
    } else {
        Locale(isoAlpha.substringBefore('-'),
            isoAlpha.substringAfter('-', ""))
    }.displayName
