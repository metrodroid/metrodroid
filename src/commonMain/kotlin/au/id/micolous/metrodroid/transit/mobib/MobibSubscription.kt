/*
 * MobibSubscription.kt
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
package au.id.micolous.metrodroid.transit.mobib

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
internal class MobibSubscription (override val parsed: En1545Parsed,
                                  private val counter: Int?): En1545Subscription() {

    private val isSubscription: Boolean
        get() = counter == 0x2f02

    override val remainingTripCount: Int?
        get() = if (isSubscription) null else counter

    override val subscriptionName: String?
        get() = if (isSubscription) Localizer.localizeString(R.string.daily_subscription) else Localizer.localizeString(R.string.single_trips)

    override val lookup: En1545Lookup
        get() = MobibLookup.instance

    constructor(dataSub: ImmutableByteArray, counter: Int?) :
            this(En1545Parser.parse(dataSub, FIELDS), counter)

    companion object {
        private val FIELDS = En1545Container(
                En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_A, 41),
                En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
                En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_B, 177)
        )
    }
}
