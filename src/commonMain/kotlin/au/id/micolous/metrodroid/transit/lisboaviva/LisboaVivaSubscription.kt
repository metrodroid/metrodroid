/*
 * LisboaVivaSubscription.kt
 *
 * Copyright 2018 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.transit.lisboaviva

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.Duration
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class LisboaVivaSubscription (override val parsed: En1545Parsed,
                              private val counter: Int?): En1545Subscription() {

    override val balance: TransitBalance?
        get() = if (!isZapping || counter == null) null else TransitCurrency.EUR(counter)

    private val isZapping: Boolean
        get() = parsed.getIntOrZero(En1545Subscription.CONTRACT_TARIFF) == LisboaVivaLookup.ZAPPING_TARIFF &&
                parsed.getIntOrZero(En1545Subscription.CONTRACT_PROVIDER) == LisboaVivaLookup.INTERAGENCY31_AGENCY

    override val lookup: En1545Lookup
        get() = LisboaVivaLookup

    override val info: List<ListItem>?
        get() = super.info.orEmpty() + ListItem(formatPeriod())

    override val validTo: Timestamp?
        get() {
            val vf = validFrom ?: return null
            val period = parsed.getIntOrZero(CONTRACT_PERIOD)
            val units = parsed.getIntOrZero(CONTRACT_PERIOD_UNITS)
            when (units) {
                0x109 -> return vf + Duration.daysLocal(period - 1)
                0x10a -> {
                    // It's calendar months. Hence this trickery
                    val ymdStart = vf.ymd
                    val ymStart = ymdStart.year * 12 + ymdStart.month.zeroBasedIndex
                    val ymEnd = ymStart + period
                    val dEnd = Daystamp(year = ymEnd / 12, month = ymEnd % 12, day = 1)
                    return dEnd + Duration.daysLocal(-1)
                }
            }
            return super.validTo
        }

    override fun getAgencyName(isShort: Boolean): FormattedString? {
        if (contractProvider == LisboaVivaLookup.INTERAGENCY31_AGENCY)
            return null
        return super.getAgencyName(isShort)
    }

    constructor(data: ImmutableByteArray, ctr: Int?) : this(En1545Parser.parse(data, SUB_FIELDS), ctr)

    private fun formatPeriod(): String {
        val period = parsed.getIntOrZero(CONTRACT_PERIOD)
        val units = parsed.getIntOrZero(CONTRACT_PERIOD_UNITS)
        when (units) {
            0x109 -> return Localizer.localizePlural(R.plurals.lisboaviva_valid_days,
                    period, period)
            0x10a -> return Localizer.localizePlural(R.plurals.lisboaviva_valid_months,
                    period, period)
        }
        return Localizer.localizeString(R.string.lisboaviva_unknown_period, period, units)
    }

    companion object {
        private const val CONTRACT_PERIOD_UNITS = "ContractPeriodUnits"
        private const val CONTRACT_PERIOD = "ContractPeriod"
        private val SUB_FIELDS = En1545Container(
                En1545FixedInteger(En1545Subscription.CONTRACT_PROVIDER, 7),
                En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 16),
                En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_A, 2),
                En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
                En1545FixedInteger(En1545Subscription.CONTRACT_SALE_AGENT, 5),
                En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_B, 19),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 16),
                En1545FixedInteger.date(En1545Subscription.CONTRACT_END),
                En1545FixedInteger(CONTRACT_PERIOD, 7),
                En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_C, 38)
        )
    }
}
