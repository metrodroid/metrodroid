/*
 * EmvData.kt
 *
 * Copyright 2019 Google
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.emv

import au.id.micolous.metrodroid.card.iso7816.TagContents.*
import au.id.micolous.metrodroid.card.iso7816.TagDesc

internal object EmvData {

    const val TAG_NAME1 = "50"
    const val T2Data = "57"
    const val TAG_NAME2 = "9f12"
    const val LOG_ENTRY = "9f4d"

    // TODO: i18n
    val TAGMAP = mapOf(
            TAG_NAME1 to TagDesc("Name 1", ASCII),
            "56" to TagDesc("Track 1", ASCII),
            T2Data to TagDesc("Track 2", DUMP_SHORT),
            "5a" to TagDesc("PAN", DUMP_SHORT), // TODO: group by 4
            "5f20" to TagDesc("Cardholder Name", ASCII),
            "5f24" to TagDesc("Expiry", DUMP_SHORT), // TODO: show as date
            "5f25" to TagDesc("Effective Date", DUMP_SHORT), // TODO: show as date
            "5f28" to TagDesc("Issuer country", COUNTRY),
            "5f2d" to TagDesc("Language preference", ASCII), // TODO: show language
            "5f34" to TagDesc("PAN sequence number", DUMP_SHORT), // TODO: show as int
            "82" to TagDesc("Application Interchange Profile", DUMP_SHORT),
            "87" to TagDesc("Application Priority Indicator", DUMP_SHORT), // TODO: show as int
            "8c" to TagDesc("CDOL1", HIDE),
            "8d" to TagDesc("CDOL2", HIDE),
            "8e" to TagDesc("CVM list", HIDE),
            "8f" to TagDesc("Certification Authority Public Key Index", DUMP_SHORT), // TODO: show as int
            "90" to TagDesc("Issuer Public Key Certificate", DUMP_LONG),
            "92" to TagDesc("Issuer Public Key Remainder", DUMP_LONG),
            "93" to TagDesc("Signed Static Application Data", DUMP_LONG),
            "94" to TagDesc("Application File Locator", DUMP_SHORT),
            "9f07" to TagDesc("Application Usage Control", HIDE),
            "9f08" to TagDesc("Application Version Number", HIDE),
            "9f0d" to TagDesc("Issuer Action Code - Default", HIDE),
            "9f0e" to TagDesc("Issuer Action Code - Denial", HIDE),
            "9f0f" to TagDesc("Issuer Action Code - Online", HIDE),
            "9f10" to TagDesc("Issuer Application Data", DUMP_LONG),
            "9f11" to TagDesc("Issuer Code Table Index", DUMP_SHORT), // TODO: show as int
            TAG_NAME2 to TagDesc("Name 2", ASCII),
            "9f1f" to TagDesc("Track 1 Discretionary Data", ASCII),
            "9f26" to TagDesc("Application Cryptogram", DUMP_LONG),
            "9f27" to TagDesc("Cryptogram Information Data", DUMP_LONG),
            "9f32" to TagDesc("Issuer Public Key Exponent", DUMP_LONG),
            "9f36" to TagDesc("Application Transaction Counter", DUMP_SHORT), // TODO: show as int
            "9f38" to TagDesc("PDOL", HIDE),
            "9f42" to TagDesc("Application currency", CURRENCY),
            "9f44" to TagDesc("Application currency exponent", DUMP_SHORT), // TODO: show currency
            "9f46" to TagDesc("ICC Public Key Certificate", DUMP_LONG),
            "9f47" to TagDesc("ICC Public Key Exponent", DUMP_LONG),
            "9f48" to TagDesc("ICC Public Key Remainder", DUMP_LONG),
            "9f49" to TagDesc("DDOL", HIDE),
            "9f4a" to TagDesc("Static Data Authentication Tag List", HIDE),
            LOG_ENTRY to TagDesc("Log entry", HIDE),
            "bf0c" to TagDesc("Subtag", HIDE)
    )
}