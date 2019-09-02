/*
 * HafilatSubscription.kt
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

package au.id.micolous.metrodroid.transit.hafilat

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed
import au.id.micolous.metrodroid.transit.en1545.En1545Parser
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription
import au.id.micolous.metrodroid.transit.intercode.IntercodeSubscription
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class HafilatSubscription (override val parsed: En1545Parsed): En1545Subscription() {
    override val lookup: HafilatLookup
        get() = HafilatLookup

    val isPurse: Boolean
        get() = lookup.isPurseTariff(contractProvider, contractTariff)

    internal constructor(data: ImmutableByteArray) : this(En1545Parser.parse(data, IntercodeSubscription.subFieldsType46))
}
