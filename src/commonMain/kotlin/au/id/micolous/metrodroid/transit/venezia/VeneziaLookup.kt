/*
 * RavKavLookup.kt
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

package au.id.micolous.metrodroid.transit.venezia

import au.id.micolous.metrodroid.time.MetroTimeZone

import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR

object VeneziaLookup : En1545LookupSTR("venezia") {

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?) = subs[contractTariff]
            ?: contractTariff.toString()

    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

    override val timeZone get() = MetroTimeZone.ROME

    override fun getMode(agency: Int?, route: Int?) = Trip.Mode.OTHER

    private val subs = mapOf(
            11105 to "Ticket 24h",
            11209 to "Rete Unica 75'",
            11210 to "Rete Unica 100'",
            12101 to "Bus Ticket 75'",
            12106 to "Airport bus ticket",
            11400 to "Carnet Traghetto")
    internal val TZ = MetroTimeZone.ROME
}
