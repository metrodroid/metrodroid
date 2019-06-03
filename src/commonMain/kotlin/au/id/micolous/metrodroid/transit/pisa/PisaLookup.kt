/*
 * PisaLookup.kt
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.transit.pisa

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR

object PisaLookup : En1545LookupSTR("pisa") {
	override fun getSubscriptionName(agency: Int?, contractTariff: Int?) = subs[contractTariff]?.second ?: contractTariff.toString()

	fun subscriptionUsesCounter(agency: Int?, contractTariff: Int?) = subs[contractTariff]?.first ?: true

	override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

	override val timeZone get() = MetroTimeZone.ROME

	override fun getMode(agency: Int?, route: Int?) = Trip.Mode.OTHER

	private val subs = mapOf(
		316 to Pair(false, "ABB ANN PERS PISA SBE"),
		317 to Pair(false, "ABB MENS PERS PISA SBE"),
		322 to Pair(true, "CARNET 10 BGL 70 MIN PISA"),
		385 to Pair(false, "ABB. TRIM. PERS. PISA")
	)
}
