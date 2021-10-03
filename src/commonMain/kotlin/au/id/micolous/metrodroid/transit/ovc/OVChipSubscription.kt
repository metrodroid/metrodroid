/*
 * OVCSubscription.kt
 *
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2012 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.transit.ovc

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class OVChipSubscription internal constructor(override val parsed: En1545Parsed,
                                                   private val mType1: Int,
                                                   private val mUsed: Int) : En1545Subscription() {

    override val subscriptionState get(): SubscriptionState =
            if (mType1 != 0) {
                if (mUsed != 0) SubscriptionState.USED else SubscriptionState.STARTED
            } else SubscriptionState.INACTIVE

    override val lookup get() = OvcLookup

    companion object {
        private fun neverSeen(i: Int) = "NeverSeen$i"

        // Sizes fully invented
        private fun neverSeenField(i: Int) = En1545FixedInteger(neverSeen(i), 8)
        fun fields(reversed: Boolean = false) = En1545Container(
                En1545Bitmap(
                        neverSeenField(1),
                        En1545FixedInteger(CONTRACT_PROVIDER, 16),
                        En1545FixedInteger(CONTRACT_TARIFF, 16),
                        En1545FixedInteger(CONTRACT_SERIAL_NUMBER, 32),
                        neverSeenField(5),
                        En1545FixedInteger(CONTRACT_UNKNOWN_A, 10),
                        neverSeenField(7),
                        neverSeenField(8),
                        neverSeenField(9),
                        neverSeenField(10),
                        neverSeenField(11),
                        neverSeenField(12),
                        neverSeenField(13),
                        En1545Bitmap(
                                En1545FixedInteger.date(CONTRACT_START),
                                En1545FixedInteger.timeLocal(CONTRACT_START),
                                En1545FixedInteger.date(CONTRACT_END),
                                En1545FixedInteger.timeLocal(CONTRACT_END),
                                En1545FixedHex(CONTRACT_UNKNOWN_C, 53),
                                En1545FixedInteger("NeverSeenB", 8),
                                En1545FixedInteger("NeverSeenC", 8),
                                En1545FixedInteger("NeverSeenD", 8),
                                En1545FixedInteger("NeverSeenE", 8),
                                reversed = reversed
                        ),
                        En1545FixedHex(CONTRACT_UNKNOWN_D, 40),
                        En1545FixedInteger(CONTRACT_SALE_DEVICE, 24),
                        neverSeenField(16),
                        neverSeenField(17),
                        neverSeenField(18),
                        neverSeenField(19),
                        reversed = reversed
                )
        )

        fun parse(data: ImmutableByteArray, type1: Int, used: Int): OVChipSubscription = OVChipSubscription(
                parsed = En1545Parser.parse(data, fields()),
                mType1 = type1,
                mUsed = used)
    }
}
