/*
 * KROCAPData.kt
 *
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
package au.id.micolous.metrodroid.card.ksx6924

import au.id.micolous.metrodroid.card.iso7816.TagContents.DUMP_SHORT
import au.id.micolous.metrodroid.card.iso7816.TagContents.HIDE
import au.id.micolous.metrodroid.card.iso7816.TagDesc
import au.id.micolous.metrodroid.multi.R

object KROCAPData {
    private const val TAG_BALANCE_COMMAND = "11"
    const val TAG_SERIAL_NUMBER = "12"
    private const val TAG_AGENCY_SERIAL_NUMBER = "13"
    private const val TAG_CARD_ISSUER = "43"
    private const val TAG_TICKET_TYPE = "45"
    private const val TAG_SUPPORTED_PROTOCOLS = "47"
    private const val TAG_ADF_AID = "4f"
    private const val TAG_CARDTYPE = "50"
    private const val TAG_EXPIRY_DATE = "5f24"
    private const val TAG_DISCRETIONARY_DATA = "bf0c"
    private const val TAG_ADDITIONAL_FILE_REFERENCES = "9f10"
    
    val TAGMAP = mapOf(
            TAG_CARDTYPE to TagDesc(R.string.cardtype_header, DUMP_SHORT),
            TAG_SUPPORTED_PROTOCOLS to TagDesc(R.string.supported_protocols, DUMP_SHORT),
            TAG_CARD_ISSUER to TagDesc(R.string.card_issuer, DUMP_SHORT),
            TAG_BALANCE_COMMAND to TagDesc(R.string.balance_command, DUMP_SHORT),
            TAG_ADF_AID to TagDesc(R.string.adf_aid, DUMP_SHORT),
            TAG_ADDITIONAL_FILE_REFERENCES to TagDesc(R.string.additional_file_references, DUMP_SHORT),
            TAG_TICKET_TYPE to TagDesc(R.string.ticket_type, DUMP_SHORT),
            TAG_EXPIRY_DATE to TagDesc(R.string.expiry_date, DUMP_SHORT),
            TAG_SERIAL_NUMBER to TagDesc(R.string.card_serial_number, HIDE),
            TAG_AGENCY_SERIAL_NUMBER to TagDesc(R.string.agency_card_serial_number, DUMP_SHORT),
            TAG_DISCRETIONARY_DATA to TagDesc(R.string.discretionary_data, DUMP_SHORT)
    )
}