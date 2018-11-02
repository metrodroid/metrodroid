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
import kotlinx.android.parcel.Parcelize
import java.util.*
import au.id.micolous.metrodroid.util.StationTableReader

const val STR = "rkf"

@Parcelize
data class RkfLookup (val mCurrencyCode : Int, val mCompany : Int) : En1545LookupSTR(STR), Parcelable {
    override fun parseCurrency(price: Int) : TransitCurrency {
        val intendedDivisor = when (mCurrencyCode shr 12) {
            0 -> 1
            1 -> 10
            2 -> 100
            9 -> 2
            else -> 1
        }
        return when (mCurrencyCode and 0xfff) {
            0x208 -> TransitCurrency((price * 100) / intendedDivisor, "DKK")
            0x578 -> TransitCurrency((price * 100) / intendedDivisor, "NOK")
            0x752 -> TransitCurrency((price * 100) / intendedDivisor, "SEK")
            0x978 -> TransitCurrency.EUR((price * 100) / intendedDivisor)
            // Fallback
            else -> TransitCurrency.USD((price * 100) / intendedDivisor)
        }
    }
    override fun getTimeZone() : TimeZone = when (mCompany / 1000) {
        0 -> TimeZone.getTimeZone("Europe/Stockholm")
        1 -> TimeZone.getTimeZone("Europe/Oslo")
        2 -> TimeZone.getTimeZone("Europe/Copenhagen")
        // Fallback
        else -> TimeZone.getTimeZone("Europe/Stockholm")
    }

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
        const val REJSEKORT = 2000
    }
}