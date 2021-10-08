package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.FormattedString
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

/**
 * - Some currency formatters return too many or too few fractional amounts. (issue #34)
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
    } else {
        // No formatting information is available.
        currencyFormatter = NumberFormat.getNumberInstance()
    }

    val amount = value.toDouble() / divisor

    if (!isBalance && value < 0) {
        // Top-ups and refunds get an explicit "positive" marker added, as this list shows
        // debits.
        return FormattedString("+ " + currencyFormatter.format(Math.abs(amount)))
    } else {
        return FormattedString(currencyFormatter.format(amount))
    }
}
