/*
 * OpusLookup.kt
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

package au.id.micolous.metrodroid.transit.opus

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.StationTableReader

class OpusLookup private constructor() : En1545LookupSTR(OPUS_STR) {

    override val timeZone: MetroTimeZone
        get() = MetroTimeZone.MONTREAL

    // For opus we ignore transport
    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        var agency = agency
        if (routeNumber == null || routeNumber == 0)
            return null
        if (agency == null)
            agency = 0
        return StationTableReader.getLineName(OPUS_STR, routeNumber or (agency shl 16))
    }

    // Opus doesn't store stations
    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? = null

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?): String? {
        if (contractTariff == null)
            return null
        val resId = SUBSCRIPTIONS[contractTariff]
        return if (resId != null) Localizer.localizeString(resId) else Localizer.localizeString(R.string.unknown_format, contractTariff)
    }

    override fun parseCurrency(price: Int) = TransitCurrency.CAD(price)

    companion object {
        private const val OPUS_STR = "opus"

        private val SUBSCRIPTIONS = mapOf(
                0xb1 to R.string.monthly_subscription,
                0xb2 to R.string.weekly_subscription,
                0x1c7 to R.string.single_trips
        )

        val instance = OpusLookup()
    }
}
