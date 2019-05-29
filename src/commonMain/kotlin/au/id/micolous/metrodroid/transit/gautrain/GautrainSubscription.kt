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

package au.id.micolous.metrodroid.transit.gautrain

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.transit.ovc.OVChipSubscription
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class GautrainSubscription internal constructor(override val parsed: En1545Parsed,
                                                     private val mType1: Int,
                                                     private val mUsed: Int) : En1545Subscription() {
    override val subscriptionState get(): Subscription.SubscriptionState =
            if (mType1 != 0) {
                if (mUsed != 0) Subscription.SubscriptionState.USED else Subscription.SubscriptionState.STARTED
            } else Subscription.SubscriptionState.INACTIVE

    override val lookup get() = GautrainLookup

    companion object {
        fun parse(data: ImmutableByteArray, type1: Int, used: Int): GautrainSubscription = GautrainSubscription(
                parsed = En1545Parser.parse(data, OVChipSubscription.fields(true)),
                mType1 = type1,
                mUsed = used)
    }
}
