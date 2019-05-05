/*
 * NextfareSubscription.java
 *
 * Copyright 2016-2017 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.nextfare

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareBalanceRecord
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTravelPassRecord

/**
 * Represents a Nextfare travel pass.
 */

@Parcelize
class NextfareSubscription (
        override val validTo: Timestamp? = null,
        override val subscriptionState: SubscriptionState): Subscription(), Parcelable {
    override val subscriptionName: String?
        get() = null

    constructor(record: NextfareTravelPassRecord) : this(
            record.timestamp, SubscriptionState.STARTED)

    // Used when there is a subscription on the card that is not yet active.
    // TODO: Figure out subscription types
    constructor(record: NextfareBalanceRecord) : this(
            subscriptionState = SubscriptionState.UNUSED)
}
