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
import au.id.micolous.metrodroid.card.iso7816.ISO7816Data.TAG_DISCRETIONARY_DATA
import au.id.micolous.metrodroid.card.iso7816.TagContents.*
import au.id.micolous.metrodroid.card.iso7816.TagDesc
import au.id.micolous.metrodroid.card.iso7816.TagHiding.CARD_NUMBER
import au.id.micolous.metrodroid.card.iso7816.TagHiding.DATE
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.ImmutableByteArray

internal object EmvData {

    const val TAG_NAME1 = "50"
    const val TAG_TRACK1 = "56"
    const val TAG_TRACK2_EQUIV = "57"
    const val TAG_TRACK3_EQUIV = "58"
    const val TAG_TRANSACTION_CURRENCY_CODE = "5f2a"
    const val TAG_TERMINAL_VERIFICATION_RESULTS = "95"
    const val TAG_TRANSACTION_DATE = "9a"
    const val TAG_TRANSACTION_TYPE = "9c"
    const val TAG_AMOUNT_AUTHORISED = "9f02"
    const val TAG_AMOUNT_OTHER = "9f03"
    const val TAG_TRANSACTION_TIME = "9f21"
    const val TAG_NAME2 = "9f12"
    const val TAG_TERMINAL_COUNTRY_CODE = "9f1a"
    const val TAG_UNPREDICTABLE_NUMBER = "9f37"
    const val TAG_PDOL = "9f38"
    const val LOG_ENTRY = "9f4d"
    const val TAG_TERMINAL_TRANSACTION_QUALIFIERS = "9f66"
    const val TAG_TRACK2 = "9f6b"

    val TAGMAP = mapOf(
            TAG_NAME1 to TagDesc(R.string.emv_name_1, ASCII),
            TAG_TRACK1 to TagDesc(R.string.emv_track_1, ASCII, CARD_NUMBER),
            TAG_TRACK2_EQUIV to TagDesc(R.string.emv_track_2_equiv, DUMP_SHORT, CARD_NUMBER),
            TAG_TRACK3_EQUIV to TagDesc(R.string.emv_track_3_equiv, DUMP_SHORT, CARD_NUMBER),
            "5a" to HIDDEN_TAG, // PAN, shown elsewhere
            "5f20" to TagDesc(R.string.card_holders_name, ASCII, CARD_NUMBER),
            // TODO: show as date
            "5f24" to TagDesc(R.string.expiry_date, DUMP_SHORT, DATE),
            // TODO: show as date
            "5f25" to TagDesc(R.string.issue_date, DUMP_SHORT, DATE),
            "5f28" to TagDesc(R.string.issuer_country, COUNTRY),
            // TODO: show language
            "5f2d" to TagDesc(R.string.emv_language_preference, ASCII),
            // TODO: show as int
            "5f34" to TagDesc(R.string.emv_pan_sequence_number, DUMP_SHORT, CARD_NUMBER),
            "82" to TagDesc(R.string.emv_application_interchange_profile, DUMP_SHORT),
            // TODO: show as int
            "87" to TagDesc(R.string.emv_application_priority_indicator, DUMP_SHORT),
            "8c" to HIDDEN_TAG, // CDOL1
            "8d" to HIDDEN_TAG, // CDOL2
            "8e" to HIDDEN_TAG, // CVM list
            // TODO: show as int
            "8f" to TagDesc(R.string.emv_ca_public_key_index, DUMP_SHORT, CARD_NUMBER),
            "90" to TagDesc(R.string.emv_issuer_public_key_certificate, DUMP_LONG, CARD_NUMBER),
            "92" to TagDesc(R.string.emv_issuer_public_key_modulus, DUMP_LONG, CARD_NUMBER),
            "93" to TagDesc(R.string.emv_signed_static_application_data, DUMP_LONG, CARD_NUMBER),
            "94" to TagDesc(R.string.emv_application_file_locator, DUMP_SHORT),
            "9f07" to HIDDEN_TAG, // Application Usage Control
            "9f08" to HIDDEN_TAG, // Application Version Number
            "9f0b" to TagDesc(R.string.card_holders_name, ASCII, CARD_NUMBER),
            "9f0d" to HIDDEN_TAG, // Issuer Action Code - Default
            "9f0e" to HIDDEN_TAG, // Issuer Action Code - Denial
            "9f0f" to HIDDEN_TAG, // Issuer Action Code - Online
            "9f10" to TagDesc(R.string.emv_issuer_application_data, DUMP_LONG, CARD_NUMBER),
            // TODO: show as int
            "9f11" to TagDesc(R.string.emv_issuer_code_table_index, DUMP_SHORT, CARD_NUMBER),
            TAG_NAME2 to TagDesc(R.string.emv_name_2, ASCII),
            "9f1f" to TagDesc(R.string.emv_track_1_discretionary_data, ASCII, CARD_NUMBER),
            "9f26" to TagDesc(R.string.emv_application_cryptogram, DUMP_LONG, CARD_NUMBER),
            "9f27" to TagDesc(R.string.emv_cryptogram_information_data, DUMP_LONG, CARD_NUMBER),
            "9f32" to TagDesc(R.string.emv_issuer_public_key_exponent, DUMP_LONG, CARD_NUMBER),
            // TODO: show as int
            "9f36" to TagDesc(R.string.emv_application_transaction_counter, DUMP_SHORT, CARD_NUMBER),
            TAG_PDOL to HIDDEN_TAG, // PDOL
            "9f42" to TagDesc(R.string.emv_application_currency, CURRENCY),
            // TODO: show currency
            "9f44" to TagDesc(R.string.emv_application_currency_exponent, DUMP_SHORT),
            "9f46" to TagDesc(R.string.emv_icc_public_key_certificate, DUMP_LONG, CARD_NUMBER),
            "9f47" to TagDesc(R.string.emv_icc_public_key_exponent, DUMP_LONG, CARD_NUMBER),
            "9f48" to TagDesc(R.string.emv_icc_public_key_modulus, DUMP_LONG, CARD_NUMBER),
            "9f49" to HIDDEN_TAG, // DDOL
            "9f4a" to HIDDEN_TAG, // Static Data Authentication Tag List
            LOG_ENTRY to HIDDEN_TAG, // Log entry
            TAG_TRACK2 to TagDesc(R.string.emv_track_2, DUMP_SHORT, CARD_NUMBER),
            TAG_DISCRETIONARY_DATA to HIDDEN_TAG // Subtag
    )

    /**
     * This instructs the EMV card parser to ignore these AID prefixes on EMV cards.
     *
     * This doesn't actually prevent dumping these sections of the card.
     *
     * This is a list of prefixes, per ISO 7816 AID behaviour where a prefix will correspond to a
     * company or country.
     */
    val PARSER_IGNORED_AID_PREFIX = listOf(
            // eftpos (Australia)
            // Has limited data. Most Australian cards also have a Mastercard or Visa application,
            // which has much more data. Few cards in the last 10 years are EFTPOS-only.
            ImmutableByteArray.fromHex("a000000384")
    )
}