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

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.StationTableReader

private const val LISBOA_VIVA_STR = "lisboa_viva"

object LisboaVivaLookup : En1545LookupSTR(LISBOA_VIVA_STR) {

    override val timeZone: MetroTimeZone
        get() = MetroTimeZone.LISBON

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): FormattedString? {
        if (routeNumber == null || routeNumber == 0)
            return null

        if (agency == null || agency == AGENCY_CARRIS)
            return FormattedString((routeNumber and 0xfff).toString())
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
        if (agency == AGENCY_METRO)
            station = station shr 2
        return StationTableReader.getStation(LISBOA_VIVA_STR,
                station or (mungedRouteNumber shl 8) or (agency shl 24),
                "$agency/$routeNumber/$station")
    }

    private fun mungeRouteNumber(agency: Int, routeNumber: Int): Int {
        if (agency == 16)
            return routeNumber and 0xf
        return if (agency == AGENCY_CP && routeNumber != ROUTE_CASCAIS_SADO) 4096 else routeNumber
    }

    override fun parseCurrency(price: Int): TransitCurrency {
        return TransitCurrency.EUR(price)
    }

    const val ZAPPING_TARIFF = 33592
    const val ZAPPING_AGENCY = 31
    const val AGENCY_CARRIS = 1
    const val AGENCY_METRO = 2
    const val AGENCY_CP = 3
    const val ROUTE_CASCAIS_SADO = 40960

    override val subscriptionMapByAgency: Map<Pair<Int?, Int>, StringResource> = mapOf(
        Pair(15, 73) to R.string.lisboaviva_sub_ass_pal_lis,
        Pair(15, 193) to R.string.lisboaviva_sub_ass_fog_lis,
        Pair(15, 217) to R.string.lisboaviva_sub_ass_pra_lis,
        Pair(16, 5) to R.string.lisboaviva_sub_passe_mts,
        Pair(30, 113) to R.string.lisboaviva_sub_metro_rl_12,
        Pair(30, 316) to R.string.lisboaviva_sub_vermelho_a1,
        Pair(30, 454) to R.string.lisboaviva_sub_metro_cp_r_mouro_melecas,
        Pair(30, 720) to R.string.lisboaviva_sub_navegante_urbano,
        Pair(30, 725) to R.string.lisboaviva_sub_navegante_rede,
        Pair(30, 733) to R.string.lisboaviva_sub_navegante_sl_tcb_barreiro,
        Pair(30, 1088) to R.string.lisboaviva_sub_fertagus_pal_lis_ml,
        Pair(ZAPPING_AGENCY, ZAPPING_TARIFF) to R.string.lisboaviva_sub_zapping
    )
}
