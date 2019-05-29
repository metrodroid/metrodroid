/*
 * RavKavSubscription.kt
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

package au.id.micolous.metrodroid.transit.ravkav

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class RavKavSubscription (override val parsed: En1545Parsed,
                          private val counter: Int?): En1545Subscription() {

    override val balance: TransitBalance?
        get() = if (ctrUse != 3 || counter == null) null else TransitCurrency.ILS(counter)

    override val remainingTripCount: Int?
        get() = if (ctrUse == 2 || counter == null) counter else null

    private val ctrUse: Int
        get() {
            val tariffType = parsed.getIntOrZero(En1545Subscription.CONTRACT_TARIFF)
            return tariffType shr 6 and 0x7
        }

    override val lookup: En1545Lookup
        get() = RavKavLookup

    constructor(data: ImmutableByteArray, ctr: Int?) : this(En1545Parser.parse(data, SUB_FIELDS), ctr)
    companion object {
        private val SUB_FIELDS = En1545Container(
                En1545FixedInteger("Version", 3),
                En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
                En1545FixedInteger(En1545Subscription.CONTRACT_PROVIDER, 8),
                En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 11),
                En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
                En1545FixedInteger(En1545Subscription.CONTRACT_SALE_DEVICE, 12),
                En1545FixedInteger("ContractSaleNumber", 10),
                En1545FixedInteger(En1545Subscription.CONTRACT_INTERCHANGE, 1),
                En1545Bitmap(
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_A, 5),
                        En1545FixedInteger(En1545Subscription.CONTRACT_RESTRICT_CODE, 5),
                        En1545FixedInteger("ContractRestrictDuration", 6),
                        En1545FixedInteger.date(En1545Subscription.CONTRACT_END),
                        En1545FixedInteger(En1545Subscription.CONTRACT_DURATION, 8),
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_B, 32),
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_C, 6),
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_D, 32),
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_E, 32)
                )
                // TODO: parse locations?
        )
    }
}
