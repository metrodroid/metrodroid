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

import android.os.Parcelable
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*

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
                Utils.convertBCDtoInteger(mCurrencyCode and 0xfff),
                intendedDivisor)
    }

    override fun getTimeZone(): TimeZone = TimeZone.getTimeZone(when (mCompany / 1000) {
        // FIXME: mCompany is an AID from the TCCI, and these are special values that aren't used?
        0 -> "Europe/Stockholm"
        1 -> "Europe/Oslo"
        2 -> "Europe/Copenhagen"

        // Per RKF-0019 "Table of Existing Application Identifiers"
        // 064 - 1f3: Swedish public transport authorisations acting on county or municipal levels
        // 1f4 - 3e7: Swedish public transport authorisations acting on national or regional levels
        //            and other operators
        in 0x64..0x3e7 -> "Europe/Stockholm"

        // Norwegian public transport authorisations or other operators
        in 0x3e8..0x7cf -> "Europe/Oslo"

        // Danish public transport authorisations or other operators
        in 0x7d0..0xbb7 -> "Europe/Copenhagen"

        // Fallback
        else -> "Europe/Stockholm"
    })

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == null)
            return null
        val routeId = routeNumber or ((agency ?: 0) shl 16)
        var routeReadable = "0x${routeNumber.toString(16)}"
        if (routeVariant != null) {
            routeReadable += "/0x${routeVariant.toString(16)}"
        }
        return StationTableReader.getLineName(STR, routeId, routeReadable)
    }

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (station == 0)
            return null
        return StationTableReader.getStation(
                STR,
                station or ((agency ?: 0) shl 16),
                (if (agency != null) "0x${agency.toString(16)}/" else "") +
                        "0x${station.toString(16)}")

    }

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?) = (
            if (contractTariff != null)
                "0x${contractTariff.toString(16)}" else "none")

    companion object {
        const val SLACCESS = 101
        const val REJSEKORT = 2000
    }
}