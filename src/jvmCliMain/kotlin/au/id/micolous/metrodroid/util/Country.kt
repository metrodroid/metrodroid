@file:JvmName("CountryKtActualJvmCli")
package au.id.micolous.metrodroid.util

import java.util.*

actual fun currencyNameBySymbol(symbol: String): String? =
    Currency.getInstance(symbol)?.displayName
