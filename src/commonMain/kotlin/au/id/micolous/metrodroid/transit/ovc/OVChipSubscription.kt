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

    override val subscriptionState get(): Subscription.SubscriptionState =
            if (mType1 != 0) {
                if (mUsed != 0) Subscription.SubscriptionState.USED else Subscription.SubscriptionState.STARTED
            } else Subscription.SubscriptionState.INACTIVE

    override val lookup get() = OvcLookup.instance

    companion object {
        private val OVC_CONTRACT_FIELDS = En1545Container(
                En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_B, 4),
                En1545Bitmap(
                        // Sizes fully invented
                        En1545FixedInteger("NeverSeen0", 8),
                        En1545FixedInteger("NeverSeen1", 8),
                        En1545FixedInteger("NeverSeen2", 8),
                        En1545FixedInteger("NeverSeen3", 8),
                        En1545FixedInteger("NeverSeen4", 8),
                        En1545FixedInteger("NeverSeen5", 8),
                        En1545FixedInteger("NeverSeen6", 8),
                        En1545FixedInteger("NeverSeen7", 8),
                        En1545FixedInteger("NeverSeen8", 8),
                        // 9
                        En1545FixedInteger(En1545Subscription.CONTRACT_PROVIDER, 8),
                        // 10
                        En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 16),
                        // 11
                        En1545FixedInteger(En1545Subscription.CONTRACT_SERIAL_NUMBER, 32),
                        // 12
                        En1545FixedInteger("NeverSeen12", 8),
                        // 13
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_A, 10),
                        En1545FixedInteger("NeverSeen14", 8),
                        En1545FixedInteger("NeverSeen15", 8),
                        En1545FixedInteger("NeverSeen16", 8),
                        En1545FixedInteger("NeverSeen17", 8),
                        En1545FixedInteger("NeverSeen18", 8),
                        En1545FixedInteger("NeverSeen19", 8),
                        En1545FixedInteger("NeverSeen20", 8),
                        En1545Bitmap(
                                En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
                                En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_START),
                                En1545FixedInteger.date(En1545Subscription.CONTRACT_END),
                                En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_END),
                                En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_C, 53),
                                En1545FixedInteger("NeverSeenB", 8),
                                En1545FixedInteger("NeverSeenC", 8),
                                En1545FixedInteger("NeverSeenD", 8),
                                En1545FixedInteger("NeverSeenE", 8)
                        ),
                        En1545FixedInteger("NeverSeen22", 8),
                        En1545FixedInteger(En1545Subscription.CONTRACT_SALE_DEVICE, 24)
                )
        )

        fun parse(data: ImmutableByteArray, type1: Int, used: Int): OVChipSubscription = OVChipSubscription(
                parsed = En1545Parser.parse(data, OVC_CONTRACT_FIELDS),
                mType1 = type1,
                mUsed = used)
    }
}
