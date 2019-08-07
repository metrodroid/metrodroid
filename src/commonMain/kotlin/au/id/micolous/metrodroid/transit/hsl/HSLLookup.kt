/*
 * HSLLookup.kt
 *
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.transit.hsl

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupUnknown
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed

object HSLLookup : En1545LookupUnknown() {
    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)
    override val timeZone: MetroTimeZone
        get() = MetroTimeZone.HELSINKI

    fun contractAreaTypeName(prefix: String) = "${prefix}AreaType"

    fun contractAreaName(prefix: String) = "${prefix}Area"

    fun languageCode(input: Int?) = when (input) {
        0 -> Localizer.localizeString(R.string.hsl_finnish)
        1 -> Localizer.localizeString(R.string.hsl_swedish)
        2 -> Localizer.localizeString(R.string.hsl_english)
        else -> Localizer.localizeString(R.string.unknown_format, input.toString())
    }
    
    private val areaMap = mapOf(
        Pair(0, 1) to R.string.hsl_region_helsinki,
        Pair(0, 2) to R.string.hsl_region_espoo,
        Pair(0, 4) to R.string.hsl_region_vantaa,
        Pair(0, 5) to R.string.hsl_region_seutu,
        Pair(0, 6) to R.string.hsl_region_kirkkonummi_siuntio,
        Pair(0, 7) to R.string.hsl_region_vihti,
        Pair(0, 8) to R.string.hsl_region_nurmijarvi,
        Pair(0, 9) to R.string.hsl_region_kerava_sipoo_tuusula,
        Pair(0, 10) to R.string.hsl_region_sipoo,
        Pair(0, 14) to R.string.hsl_region_lahiseutu_2,
        Pair(0, 15) to R.string.hsl_region_lahiseutu_3,
        Pair(1, 1) to R.string.hsl_transport_bussi,
        Pair(1, 2) to R.string.hsl_transport_bussi_2,
        Pair(1, 3) to R.string.hsl_transport_bussi_3,
        Pair(1, 4) to R.string.hsl_transport_bussi_4,
        Pair(1, 5) to R.string.hsl_transport_raitiovaunu,
        Pair(1, 6) to R.string.hsl_transport_metro,
        Pair(1, 7) to R.string.hsl_transport_juna,
        Pair(1, 8) to R.string.hsl_transport_lautta,
        Pair(1, 9) to R.string.hsl_transport_u_linja
    )

    fun getArea(parsed: En1545Parsed, prefix: String, isValidity: Boolean): String? {
        val type = parsed.getIntOrZero(contractAreaTypeName(prefix))
        val value = parsed.getIntOrZero(contractAreaName(prefix))
        if (type in 0..1 && value == 0) {
            return null
        }
        if (type == 2) {
            val to = value and 7
            if (isValidity) {
                val from = value shr 3
                return Localizer.localizePlural(R.plurals.hsl_zones, to - from + 1, String((from..to).map { 'A' + it }.toCharArray()))
            } else {
                return Localizer.localizeString(R.string.hsl_zone_station, String(charArrayOf('A' + to)))
            }
        }
        return areaMap[Pair(type, value)]?.let {
            Localizer.localizeString(it)
        } ?: Localizer.localizeString(R.string.unknown_format, "$type/$value")
    }
}
