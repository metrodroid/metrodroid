/*
 * HafilatLookup.kt
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

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.NumberUtils

object HafilatLookup : En1545LookupSTR("hafilat") {

    override val timeZone: MetroTimeZone
        get() = MetroTimeZone.DUBAI

    override fun parseCurrency(price: Int): TransitCurrency = TransitCurrency(price, "AED")

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?): String? {
        if (contractTariff == null)
            return null
        val tariff = TARIFFS[contractTariff] ?: return NumberUtils.intToHex(contractTariff)

        return Localizer.localizeString(tariff)
    }

    internal fun isPurseTariff(agency: Int?, contractTariff: Int?): Boolean = agency == 1 && contractTariff in listOf(0x2710)

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? = "$routeNumber"

    private val TARIFFS = mapOf(
            0x2710 to R.string.adelaide_ticket_type_regular
            // TODO: handle other tickets
    )
}
