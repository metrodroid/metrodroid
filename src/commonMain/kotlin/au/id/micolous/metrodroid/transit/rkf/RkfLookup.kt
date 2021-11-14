/*
 * RkfLookup.kt
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

package au.id.micolous.metrodroid.transit.rkf

import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.hexString

const val STR = "rkf"

@Parcelize
data class RkfLookup(val mCurrencyCode: Int, val mCompany: Int) : En1545LookupSTR(STR), Parcelable {
    override fun parseCurrency(price: Int): TransitCurrency {
        val intendedDivisor = when (mCurrencyCode shr 12) {
            0 -> 1
            1 -> 10
            2 -> 100
            9 -> 2
            else -> 1
        }

        return TransitCurrency(price,
                NumberUtils.convertBCDtoInteger(mCurrencyCode and 0xfff),
                intendedDivisor)
    }

    override val timeZone get(): MetroTimeZone = when (mCompany / 1000) {
        // FIXME: mCompany is an AID from the TCCI, and these are special values that aren't used?
        0 -> MetroTimeZone.STOCKHOLM
        1 -> MetroTimeZone.OSLO
        2 -> MetroTimeZone.COPENHAGEN

        // Per RKF-0019 "Table of Existing Application Identifiers"
        // 064 - 1f3: Swedish public transport authorisations acting on county or municipal levels
        // 1f4 - 3e7: Swedish public transport authorisations acting on national or regional levels
        //            and other operators
        in 0x64..0x3e7 -> MetroTimeZone.STOCKHOLM

        // Norwegian public transport authorisations or other operators
        in 0x3e8..0x7cf -> MetroTimeZone.OSLO

        // Danish public transport authorisations or other operators
        in 0x7d0..0xbb7 -> MetroTimeZone.COPENHAGEN

        // Fallback
        else -> MetroTimeZone.STOCKHOLM
    }

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): FormattedString? {
        if (routeNumber == null)
            return null
        val routeId = routeNumber or ((agency ?: 0) shl 16)
        val routeReadable = getHumanReadableRouteId(routeNumber, routeVariant, agency, transport)
        return StationTableReader.getLineName(STR, routeId, routeReadable!!)
    }

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (station == 0)
            return null
        return StationTableReader.getStation(
                STR,
                station or ((agency ?: 0) shl 16),
                (if (agency != null) "${agency.hexString}/" else "") +
                        station.hexString)

    }

    override val subscriptionMapByAgency: Map<Pair<Int?, Int>, StringResource> get() = mapOf(
        Pair(SLACCESS, 1022) to R.string.rkf_stockholm_30_days,
        Pair(SLACCESS, 1184) to R.string.rkf_stockholm_7_days,
        Pair(SLACCESS, 1225) to R.string.rkf_stockholm_72_hours
    )

    companion object {
        const val SLACCESS = 101
        const val REJSEKORT = 2000
    }
}
