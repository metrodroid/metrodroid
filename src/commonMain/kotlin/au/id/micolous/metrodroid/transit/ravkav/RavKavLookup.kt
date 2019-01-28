/*
 * RavKavLookup.java
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

package au.id.micolous.metrodroid.transit.ravkav

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR

private const val RAVKAV_STR = "ravkav"

internal object RavKavLookup : En1545LookupSTR(RAVKAV_STR) {

    override val timeZone: MetroTimeZone
        get() = MetroTimeZone.JERUSALEM

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == null || routeNumber == 0)
            return null
        if (agency != null && agency == EGGED)
            return (routeNumber % 1000).toString()
        return routeNumber.toString()
    }

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?): String? = contractTariff?.toString()
        // TODO: Figure names out

    override fun parseCurrency(price: Int)= TransitCurrency.ILS(price)

    // Irrelevant as RavKAv has EventCode
    override fun getMode(agency: Int?, route: Int?): Trip.Mode = Trip.Mode.OTHER

    private const val EGGED = 0x3
}
