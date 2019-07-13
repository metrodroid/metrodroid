/*
 * MobibLookup.kt
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

package au.id.micolous.metrodroid.transit.mobib

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.StationTableReader

internal const val MOBIB_STR = "mobib"

object MobibLookup : En1545LookupSTR(MOBIB_STR) {
    override val timeZone: MetroTimeZone
        get() = MobibTransitData.TZ

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?) =
            when (agency) {
                null -> null
                BUS, TRAM -> routeNumber?.toString()?.let { FormattedString(it) }
                else -> null
            }

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (station == 0)
            return null
        return StationTableReader.getStation(MOBIB_STR, station or ((agency ?: 0) shl 22))
    }

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?) = subs[contractTariff]
    ?: Localizer.localizeString(R.string.unknown_format, contractTariff?.toString())

    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

    override fun getAgencyName(agency: Int?, isShort: Boolean) =
        if (agency == null)
            null
        else
            StationTableReader.getOperatorName(MOBIB_STR, agency, isShort)

    const val BUS = 0xf
    const val TRAM = 0x16

    val subs = mapOf(
            0x2801 to "Jump 1 trip",
            0x2803 to "Jump 10 trips",
            0x0805 to "Airport BUS",
            0x303d to "Jump 24h + Bus Airport"
    )
}
