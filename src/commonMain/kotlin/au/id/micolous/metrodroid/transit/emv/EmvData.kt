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

import au.id.micolous.metrodroid.card.iso7816.HIDDEN_TAG
import au.id.micolous.metrodroid.card.iso7816.TagContents.*
import au.id.micolous.metrodroid.card.iso7816.TagDesc
import au.id.micolous.metrodroid.multi.R

internal object EmvData {

    const val TAG_NAME1 = "50"
    const val T2Data = "57"
    const val TAG_NAME2 = "9f12"
    const val LOG_ENTRY = "9f4d"

    // TODO: i18n
    val TAGMAP = mapOf(
            TAG_NAME1 to TagDesc(R.string.emv_name_1, ASCII),
            "56" to TagDesc(R.string.emv_track_1, ASCII),
            T2Data to TagDesc(R.string.emv_track_2, DUMP_SHORT),
            // TODO: group by 4
            "5a" to TagDesc("PAN", DUMP_SHORT),
            "5f20" to TagDesc(R.string.card_holders_name, ASCII),
            // TODO: show as date
            "5f24" to TagDesc(R.string.expiry_date, DUMP_SHORT),
            // TODO: show as date
            "5f25" to TagDesc(R.string.issue_date, DUMP_SHORT),
            "5f28" to TagDesc(R.string.issuer_country, COUNTRY),
            // TODO: show language
            "5f2d" to TagDesc(R.string.emv_language_preference, ASCII),
            // TODO: show as int
            "5f34" to TagDesc("PAN sequence number", DUMP_SHORT),
            "82" to TagDesc("Application Interchange Profile", DUMP_SHORT),
            // TODO: show as int
            "87" to TagDesc("Application Priority Indicator", DUMP_SHORT),
            "8c" to HIDDEN_TAG, // CDOL1
            "8d" to HIDDEN_TAG, // CDOL2
            "8e" to HIDDEN_TAG, // CVM list
            // TODO: show as int
            "8f" to TagDesc("Certification Authority Public Key Index", DUMP_SHORT),
            "90" to TagDesc(R.string.emv_issuer_public_key_certificate, DUMP_LONG),
            "92" to TagDesc(R.string.emv_issuer_public_key_modulus, DUMP_LONG),
            "93" to TagDesc("Signed Static Application Data", DUMP_LONG),
            "94" to TagDesc("Application File Locator", DUMP_SHORT),
            "9f07" to HIDDEN_TAG, // Application Usage Control
            "9f08" to HIDDEN_TAG, // Application Version Number
            "9f0d" to HIDDEN_TAG, // Issuer Action Code - Default
            "9f0e" to HIDDEN_TAG, // Issuer Action Code - Denial
            "9f0f" to HIDDEN_TAG, // Issuer Action Code - Online
            "9f10" to TagDesc("Issuer Application Data", DUMP_LONG),
            // TODO: show as int
            "9f11" to TagDesc("Issuer Code Table Index", DUMP_SHORT),
            TAG_NAME2 to TagDesc(R.string.emv_name_2, ASCII),
            "9f1f" to TagDesc("Track 1 Discretionary Data", ASCII),
            "9f26" to TagDesc("Application Cryptogram", DUMP_LONG),
            "9f27" to TagDesc("Cryptogram Information Data", DUMP_LONG),
            "9f32" to TagDesc(R.string.emv_issuer_public_key_exponent, DUMP_LONG),
            // TODO: show as int
            "9f36" to TagDesc("Application Transaction Counter", DUMP_SHORT),
            "9f38" to HIDDEN_TAG, // PDOL
            "9f42" to TagDesc(R.string.emv_application_currency, CURRENCY),
            // TODO: show currency
            "9f44" to TagDesc(R.string.emv_application_currency_exponent, DUMP_SHORT),
            "9f46" to TagDesc(R.string.emv_icc_public_key_certificate, DUMP_LONG),
            "9f47" to TagDesc(R.string.emv_icc_public_key_exponent, DUMP_LONG),
            "9f48" to TagDesc(R.string.emv_icc_public_key_modulus, DUMP_LONG),
            "9f49" to HIDDEN_TAG, // DDOL
            "9f4a" to HIDDEN_TAG, // Static Data Authentication Tag List
            LOG_ENTRY to HIDDEN_TAG, // Log entry
            "bf0c" to HIDDEN_TAG // Subtag
    )
}