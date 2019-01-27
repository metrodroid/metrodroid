package au.id.micolous.metrodroid.transit

import android.text.style.TtsSpan
import android.os.Build
import android.text.SpannableString
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.util.NumberUtils
import java.text.NumberFormat
import java.util.*

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
    // numberFormatter is only used for TtsSpan, so needs to give a consistent result.
    val numberFormatter = NumberFormat.getNumberInstance(Locale.ENGLISH)
    numberFormatter.isGroupingUsed = false

    val c = if (currencyCode != TransitCurrency.UNKNOWN_CURRENCY_CODE)
        Currency.getInstance(currencyCode)
    else
        null

    val currencyFormatter: NumberFormat
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
        numberFormatter.minimumFractionDigits = c.defaultFractionDigits
    } else {
        // No formatting information is available.
        currencyFormatter = NumberFormat.getNumberInstance()

        // Infer number of decimal places we should add based on the divisor
        numberFormatter.minimumFractionDigits = NumberUtils.log10floor(divisor)
    }

    val s: SpannableString
    val numberOffset: Int
    val amount: Double

    if (!isBalance && value < 0) {
        // Top-ups and refunds get an explicit "positive" marker added, as this list shows
        // debits.
        amount = Math.abs(value.toDouble() / divisor)
        s = SpannableString("+ " + currencyFormatter.format(amount))
        numberOffset = 2
    } else {
        amount = value.toDouble() / divisor
        s = SpannableString(currencyFormatter.format(amount))
        numberOffset = 0
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && c != null) {
        s.setSpan(TtsSpan.MoneyBuilder()
                .setIntegerPart(numberFormatter.format(amount))
                .setCurrency(c.currencyCode)
                .build(), numberOffset, s.length, 0)
    }

    return FormattedString(s)
}
