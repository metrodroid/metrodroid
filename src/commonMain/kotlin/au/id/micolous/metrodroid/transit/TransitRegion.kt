/*
 * TransitRegion.kt
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
package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.util.iso3166AlphaToName

sealed class TransitRegion {
    abstract val translatedName: String
    data class Iso (val code: String): TransitRegion () {
        override val translatedName: String
            get() = iso3166AlphaToName(code) ?: code
    }

    data class Custom(val res: StringResource): TransitRegion () {
        override val translatedName: String
            get() = Localizer.localizeString(res)
    }

    companion object {
        val XX = Iso("XX")
        val AUSTRALIA = Iso("AU")
        val BELGIUM = Iso("BE")
        val BRAZIL = Iso("BR")
        val CANADA = Iso("CA")
        val CHILE = Iso("CL")
        val CHINA = Iso("CN")
        val CRIMEA = Custom(R.string.location_crimea)
        val DENMARK = Iso("DK")
        val ESTONIA = Iso("EE")
        val FINLAND = Iso("FI")
        val FRANCE = Iso("FR")
        val GEORGIA = Iso("GE")
        val GERMANY = Iso("DE")
        val HONG_KONG = Iso("HK")
        val INDONESIA = Iso("ID")
        val IRELAND = Iso("IE")
        val ISRAEL = Iso("IL")
        val ITALY = Iso("IT")
        val JAPAN = Iso("JP")
        val MALAYSIA = Iso("MY")
        val NETHERLANDS = Iso("NL")
        val NEW_ZEALAND = Iso("NZ")
        val POLAND = Iso("PL")
        val PORTUGAL = Iso("PT")
        val RUSSIA = Iso("RU")
        val SINGAPORE = Iso("SG")
        val SOUTH_AFRICA = Iso("ZA")
        val SOUTH_KOREA = Iso("KR")
        val SPAIN = Iso("ES")
        val SWEDEN = Iso("SE")
        val SWITZERLAND = Iso("CH")
        val TAIPEI = Iso("TW")
        val TURKEY = Iso("TR")
        val UAE = Iso("AE")
        val UK = Iso("GB")
        val UKRAINE = Iso("UA")
        val USA = Iso("US")
        val WORLDWIDE = Custom(R.string.location_worldwide)
    }
}
