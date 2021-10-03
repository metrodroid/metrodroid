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
                parsed.getInfo(setOf(CONTRACT_TARIFF,
                        En1545FixedInteger.dateName(CONTRACT_SALE),
                        CONTRACT_SALE_DEVICE,
                        CONTRACT_PRICE_AMOUNT,
                        CONTRACT_SALE_AGENT,
                        CONTRACT_SERIAL_NUMBER,
                        CONTRACT_PROVIDER,
                        CONTRACT_STATUS)
                )

    val isPurse: Boolean
        get() = lookup.isPurseTariff(contractProvider, contractTariff)

    internal constructor(data: ImmutableByteArray) : this(En1545Parser.parse(data, IntercodeSubscription.subFieldsType46))
}
