package au.id.micolous.metrodroid.transit

import android.text.style.TtsSpan
import android.os.Build
import android.text.SpannableString
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.NumberUtils
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

/**
 * This handles Android-specific issues:
 *
 *
 * - Some currency formatters return too many or too few fractional amounts. (issue #34)
 * - Markup with TtsSpan.MoneyBuilder, for accessibility tools.
 *
 * @param isBalance True if the value being passed is a balance (ie: don't format credits in a
 * special way)
 * @return Formatted currency string
 */
internal actual fun formatCurrency(value: Int, divisor: Int, currencyCode: String, isBalance: Boolean): FormattedString {
    val c = if (currencyCode != TransitCurrency.UNKNOWN_CURRENCY_CODE)
        Currency.getInstance(currencyCode)
    else
        null

    val currencyFormatter: NumberFormat
    val minimumFractionDigits: Int
    if (c != null) {
        // We have formatting information.
        currencyFormatter = NumberFormat.getCurrencyInstance()
        currencyFormatter.currency = c

        // https://github.com/micolous/metrodroid/issues/34
        // Java's NumberFormat returns too many, or too few fractional amounts
        // in currencies, depending on the system locale.
        // In Japanese, AUD is formatted as "A$1" instead of "A$1.23".
        // In English, JPY is formatted as "¥123.00" instead of "¥123"
        currencyFormatter.minimumFractionDigits = c.defaultFractionDigits
        minimumFractionDigits = c.defaultFractionDigits
    } else {
        // No formatting information is available.
        currencyFormatter = NumberFormat.getNumberInstance()

        // Infer number of decimal places we should add based on the divisor
        minimumFractionDigits = NumberUtils.log10floor(divisor)
    }

    val s: SpannableString
    val amountPrefix: String

    if (!isBalance && value < 0) {
        // Top-ups and refunds get an explicit "positive" marker added, as this list shows
        // debits.
        s = SpannableString("+ " + currencyFormatter.format(abs(value.toDouble() / divisor)))
        amountPrefix = "+"
    } else {
        s = SpannableString(currencyFormatter.format(value.toDouble() / divisor))
        amountPrefix = if (value < 0) "-" else ""
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && c != null) {
        val intPart = amountPrefix + (abs(value) / divisor).toString()

        val builder = TtsSpan.MoneyBuilder()
                .setIntegerPart(intPart)
                .setCurrency(c.currencyCode)
        if (minimumFractionDigits != 0) {
            val fraction = NumberUtils.zeroPad((NumberUtils.pow(10, minimumFractionDigits)
                    * (abs(value) % divisor)) / divisor,
                    minDigits = minimumFractionDigits)
            builder.setFractionalPart(fraction)
            Log.d("TransitCurrency", "Money ($value/$divisor) = $intPart / $fraction")
        } else
            Log.d("TransitCurrency", "Money ($value/$divisor) = $intPart")
        s.setSpan(builder.build(), 0, s.length, 0)
    }

    return FormattedString(s)
}
