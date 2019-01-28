/*
 * RicaricaMiLookup.java
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

package au.id.micolous.metrodroid.transit.ricaricami

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR

class RicaricaMiLookup private constructor() : En1545LookupSTR("ricaricami") {

    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

    override val timeZone get() = TZ

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?) = when (contractTariff) {
        TARIFF_SINGLE_URBAN -> "Urban ticket"
        TARIFF_DAILY_URBAN -> "Urban daily ticket"
        TARIFF_URBAN_2X6 -> "Urban weekly 2x6 ticket"
        null -> null
        else -> Localizer.localizeString(R.string.unknown_format, contractTariff.toString())
    }

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == null)
            return null
        when(transport) {
            TRANSPORT_METRO -> {
                when (routeNumber) {
                    101 -> return "M1"
                    104 -> return "M2"
                    107 -> return "M5"
                    301 -> return "M3"
                }
            }
            TRANSPORT_TRENORD1, TRANSPORT_TRENORD2 -> {
                // Essentially a placeholder
                if (routeNumber == 1000)
                    return null
            }
            TRANSPORT_TRAM -> {
                if (routeNumber == 60)
                    return null
            }
        }
        if (routeVariant != null) {
            return "$routeNumber/$routeVariant"
        }
        return routeNumber.toString()
    }

    companion object {
        val TZ = MetroTimeZone.ROME
        const val TRANSPORT_METRO = 1
        const val TRANSPORT_BUS = 2
        const val TRANSPORT_TRAM = 4
        const val TRANSPORT_TRENORD1 = 7
        const val TRANSPORT_TRENORD2 = 9
        const val TARIFF_URBAN_2X6 = 0x1b39
        const val TARIFF_SINGLE_URBAN = 0xfff
        const val TARIFF_DAILY_URBAN = 0x100d

        private val sInstance = RicaricaMiLookup()

        val instance: En1545Lookup
            get() = sInstance
    }
}
