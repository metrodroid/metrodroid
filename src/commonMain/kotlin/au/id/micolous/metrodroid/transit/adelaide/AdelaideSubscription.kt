/*
 * AdelaideSubscription.kt
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

package au.id.micolous.metrodroid.transit.adelaide

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.*

import au.id.micolous.metrodroid.transit.intercode.IntercodeSubscription
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class AdelaideSubscription (override val parsed: En1545Parsed): En1545Subscription() {
    override val lookup: AdelaideLookup
        get() = AdelaideLookup

    override val info: List<ListItem>?
        get() = super.info.orEmpty() +
                parsed.getInfo(setOf(En1545Subscription.CONTRACT_TARIFF,
                        En1545FixedInteger.dateName(En1545Subscription.CONTRACT_SALE),
                        En1545Subscription.CONTRACT_SALE_DEVICE,
                        En1545Subscription.CONTRACT_PRICE_AMOUNT,
                        En1545Subscription.CONTRACT_SALE_AGENT,
                        En1545Subscription.CONTRACT_PROVIDER, En1545Subscription.CONTRACT_STATUS)
                )

    val isPurse: Boolean
        get() = lookup.isPurseTariff(contractProvider, contractTariff)

    internal constructor(data: ImmutableByteArray) : this(En1545Parser.parse(data, IntercodeSubscription.subFieldsType46))
}
