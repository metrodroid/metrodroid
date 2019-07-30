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

    fun getArea(parsed: En1545Parsed, prefix: String, isValidity: Boolean): String? {
        val type = parsed.getInt(contractAreaTypeName(prefix))
        val value = parsed.getInt(contractAreaName(prefix))
        return when (type) {
            // FIXME: i18n
            null, 0 -> when (value) {
                0 -> "Ei määritelty"
                1 -> "Helsinki"
                2 -> "Espoo"
                4 -> "Vantaa"
                5 -> "Seutu (HEL+ESP-VAN)"
                6 -> "Kirkkonummi-Siuntio"
                7 -> "Vihti"
                8 -> "Nurmijärvi"
                9 -> "Kerava-Sipoo-Tuusula"
                10 -> "Sipoo"
                14 -> "Lähiseutu 2 (ESP+VAN+KIR+KER+SIP)"
                15 -> "Lähiseutu 3 (HEL+ESP+VAN+KIR+KER+SIP)"
                null -> null
                else -> Localizer.localizeString(R.string.unknown_format, "$type/$value")
            }
            1 -> when (value) {
                0 -> "Ei määritelty"
                1 -> "Bussi"
                2 -> "Bussi 2"
                3 -> "Bussi 3"
                4 -> "Bussi 4"
                5 -> "Raitiovaunu"
                6 -> "Metro"
                7 -> "Juna"
                8 -> "Lautta"
                9 -> "U-linja"
                else -> Localizer.localizeString(R.string.unknown_format, "$type/$value")
            }
            2 -> {
                value ?: return Localizer.localizeString(R.string.unknown_format, "$type/$value")
                val to = value and 7
                if (isValidity) {
                    val from = value shr 3
                    Localizer.localizePlural(R.plurals.hsl_zones, to - from + 1, String((from..to).map { 'A' + it }.toCharArray()))
                } else {
                    Localizer.localizeString(R.string.hsl_zone_station, String(charArrayOf('A' + to)))
                }
            }
            else -> Localizer.localizeString(R.string.unknown_format, "$type/$value")
        }
    }
}