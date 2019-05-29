/*
 * LisboaVivaLookup.kt
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

package au.id.micolous.metrodroid.transit.lisboaviva

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.StationTableReader

private const val LISBOA_VIVA_STR = "lisboa_viva"

object LisboaVivaLookup : En1545LookupSTR(LISBOA_VIVA_STR) {

    override val timeZone: MetroTimeZone
        get() = MetroTimeZone.LISBON

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == null || routeNumber == 0)
            return null

        if (agency == null || agency == 1)
            return routeNumber.toString()
        val mungedRouteNumber = mungeRouteNumber(agency, routeNumber)
        return StationTableReader.getLineName(LISBOA_VIVA_STR, agency shl 16 or mungedRouteNumber,
                mungedRouteNumber.toString())
    }

    override fun getHumanReadableRouteId(routeNumber: Int?,
                                         routeVariant: Int?,
                                         agency: Int?,
                                         transport: Int?): String? {
        if (routeNumber == null || agency == null) {
            // Null route number = unknown route
            // Null agency = return raw route number (so no need to duplicate)
            return null
        }
        return mungeRouteNumber(agency, routeNumber).toString()
    }

    override fun getStation(station: Int, agency: Int?, routeNumber: Int?): Station? {
        if (station == 0 || agency == null || routeNumber == null)
            return null
        val mungedRouteNumber = mungeRouteNumber(agency, routeNumber)
        var station = station
        if (agency == 2)
            station = station shr 2
        return StationTableReader.getStation(LISBOA_VIVA_STR,
                station or (mungedRouteNumber shl 8) or (agency shl 24),
                station.toString())
    }

    private fun mungeRouteNumber(agency: Int, routeNumber: Int): Int {
        if (agency == 16)
            return routeNumber and 0xf
        return if (agency == AGENCY_CP && routeNumber != ROUTE_CASCAIS_SADO) 4096 else routeNumber
    }

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?): String? {
        if (contractTariff == null || agency == null)
            return null

        if (agency == 15) {
            when (contractTariff) {
                73 -> return "Ass. PAL - LIS"
                193 -> return "Ass. FOG - LIS"
                217 -> return "Ass. PRA - LIS"
            }
        }
        if (agency == 16 && contractTariff == 5)
            return "Passe MTS"
        if (agency == 30) {
            when (contractTariff) {
                113 -> return "Metro / RL 12"
                316 -> return "Vermelho A1"
                454 -> return "Metro/CP - R. Mouro/Meleças"
                720 -> return "Navegante urbano"
                725 -> return "Navegante rede"
                733 -> return "Navegante SL TCB Barreiro"
                1088 -> return "Fertagus PAL - LIS + ML"
            }
        }
        if (agency == ZAPPING_AGENCY && contractTariff == ZAPPING_TARIFF)
            return "Zapping"
        return contractTariff.toString()
    }

    override fun parseCurrency(price: Int): TransitCurrency {
        return TransitCurrency.EUR(price)
    }

    const val ZAPPING_TARIFF = 33592
    const val ZAPPING_AGENCY = 31
    const val AGENCY_CP = 3
    const val ROUTE_CASCAIS_SADO = 40960
}
