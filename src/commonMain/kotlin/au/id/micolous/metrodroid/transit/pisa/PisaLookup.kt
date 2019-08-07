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

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR

object PisaLookup : En1545LookupSTR("pisa") {
	fun subscriptionUsesCounter(agency: Int?, contractTariff: Int?) = contractTariff !in listOf(316, 317, 385)

	override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

	override val timeZone get() = MetroTimeZone.ROME

	override fun getMode(agency: Int?, route: Int?) = Trip.Mode.OTHER

	override val subscriptionMap = mapOf(
			316 to R.string.pisa_abb_ann_pers_pisa_sbe,
			317 to R.string.pisa_abb_mens_pers_pisa_sbe,
			322 to R.string.pisa_carnet_10_bgl_70_min_pisa,
			385 to R.string.pisa_abb_trim_pers_pisa
	)
}
