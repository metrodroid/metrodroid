/*
 * OpusSubscription.kt
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
package au.id.micolous.metrodroid.transit.opus

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
internal data class OpusSubscription(override val parsed: En1545Parsed,
                                     val ctr: Int?) : En1545Subscription() {

    override val lookup: En1545Lookup
        get() = OpusLookup

    override val remainingTripCount: Int?
        get() = if (parsed.getIntOrZero(En1545FixedInteger.dateName(En1545Subscription.CONTRACT_END)) == 0)
            ctr else null

    constructor(data: ImmutableByteArray, ctr: Int?) : this(En1545Parser.parse(data, FIELDS), ctr)

    companion object {
        private val FIELDS = En1545Container(
                En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_A, 3),
                En1545Bitmap(
                        En1545FixedInteger(En1545Subscription.CONTRACT_PROVIDER, 8),
                        En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 16),
                        En1545Bitmap(
                                En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
                                En1545FixedInteger.date(En1545Subscription.CONTRACT_END)
                        ),
                        En1545Container(
                                En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_B, 17),
                                En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
                                En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_SALE),
                                En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_C, 36),
                                En1545FixedInteger(En1545Subscription.CONTRACT_STATUS, 8),
                                En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_D, 36)
                        )
                )
        )
    }
}
