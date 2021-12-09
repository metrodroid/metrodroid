package au.id.micolous.metrodroid.util

import android.os.Build
import java.util.*

actual fun currencyNameBySymbol(symbol: String): String? =
    try {
        Currency.getInstance(symbol)?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
