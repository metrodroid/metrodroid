@file:JvmName("CountryKtActualAndroid")
package au.id.micolous.metrodroid.util

import android.os.Build
import java.util.*

actual fun currencyNameBySymbol(symbol: String): String? =
    Currency.getInstance(symbol)?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            it.displayName
        } else {
            it.currencyCode
        }
    }
