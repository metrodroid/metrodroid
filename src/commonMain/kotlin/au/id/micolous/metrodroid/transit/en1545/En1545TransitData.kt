/*
 * IntercodeTransitData.kt
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

package au.id.micolous.metrodroid.transit.en1545

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.TimestampFormatter

import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.Preferences

abstract class En1545TransitData : TransitData {

    protected val mTicketEnvParsed: En1545Parsed

    override val info: List<ListItem>?
        get() {
            val li = mutableListOf<ListItem>()
            val tz = lookup.timeZone
            if (mTicketEnvParsed.contains(ENV_NETWORK_ID))
                li.add(ListItem(R.string.en1545_network_id,
                        mTicketEnvParsed.getIntOrZero(ENV_NETWORK_ID).toString(16)))

            mTicketEnvParsed.getTimeStamp(ENV_APPLICATION_VALIDITY_END, tz)?.let {
                li.add(ListItem(R.string.expiry_date, TimestampFormatter.longDateFormat(it)))
            }

            if (!Preferences.hideCardNumbers && !Preferences.obfuscateTripDates)
                mTicketEnvParsed.getTimeStamp(HOLDER_BIRTH_DATE, tz)?.let {
                    li.add(ListItem(R.string.date_of_birth, TimestampFormatter.longDateFormat(it)))
                }

            if (mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_ISSUER_ID) != 0)
                li.add(ListItem(R.string.card_issuer,
                        lookup.getAgencyName(mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_ISSUER_ID), false)))

            mTicketEnvParsed.getTimeStamp(ENV_APPLICATION_ISSUE, tz)?.let {
                li.add(ListItem(R.string.issue_date, TimestampFormatter.longDateFormat(it)))
            }

            mTicketEnvParsed.getTimeStamp(HOLDER_PROFILE, tz)?.let {
                li.add(ListItem(R.string.en1545_card_expiry_date_profile, TimestampFormatter.longDateFormat(it)))
            }

            if (!Preferences.hideCardNumbers && !Preferences.obfuscateTripDates)
                // Only Mobib sets this, and Belgium has numeric postal codes.
                mTicketEnvParsed.getInt(HOLDER_INT_POSTAL_CODE)?.let {
                    if (it != 0)
                        li.add(ListItem(R.string.postal_code, it.toString()))
                }

            mTicketEnvParsed.getInt(HOLDER_CARD_TYPE).let {
                li.add(ListItem(R.string.card_type, when (it) {
                    0 -> R.string.card_type_anonymous
                    1 -> R.string.card_type_declarative
                    2 -> R.string.card_type_personal
                    else -> R.string.card_type_provider_specific
                }))
            }

            return li
        }

    protected abstract val lookup: En1545Lookup

    protected constructor() {
        mTicketEnvParsed = En1545Parsed()
    }

    protected constructor(parsed: En1545Parsed) {
        mTicketEnvParsed = parsed
    }

    override fun getRawFields(level: RawLevel): List<ListItem>? =
            mTicketEnvParsed.getInfo(
                    when (level) {
                        RawLevel.UNKNOWN_ONLY -> setOf(
                                ENV_NETWORK_ID,
                                En1545FixedInteger.datePackedName(ENV_APPLICATION_VALIDITY_END),
                                En1545FixedInteger.dateName(ENV_APPLICATION_VALIDITY_END),
                                En1545FixedInteger.dateBCDName(HOLDER_BIRTH_DATE),
                                ENV_APPLICATION_ISSUER_ID,
                                HOLDER_CARD_TYPE,
                                En1545FixedInteger.datePackedName(ENV_APPLICATION_ISSUE),
                                En1545FixedInteger.dateName(ENV_APPLICATION_ISSUE),
                                En1545FixedInteger.datePackedName(HOLDER_PROFILE),
                                En1545FixedInteger.dateName(HOLDER_PROFILE),
                                HOLDER_INT_POSTAL_CODE,
                                ENV_CARD_SERIAL,
                                ENV_AUTHENTICATOR
                        )
                        else -> setOf()
                    })

    companion object {
        const val ENV_NETWORK_ID = "EnvNetworkId"
        const val ENV_VERSION_NUMBER = "EnvVersionNumber"
        const val HOLDER_BIRTH_DATE = "HolderBirth"
        const val ENV_APPLICATION_VALIDITY_END = "EnvApplicationValidityEnd"
        const val ENV_APPLICATION_ISSUER_ID = "EnvApplicationIssuerId"
        const val ENV_APPLICATION_ISSUE = "EnvApplicationIssue"
        const val HOLDER_PROFILE = "HolderProfile"
        const val HOLDER_INT_POSTAL_CODE = "HolderIntPostalCode"
        const val HOLDER_CARD_TYPE = "HolderDataCardStatus"
        const val ENV_AUTHENTICATOR = "EnvAuthenticator"
        const val ENV_UNKNOWN_A = "EnvUnknownA"
        const val ENV_UNKNOWN_B = "EnvUnknownB"
        const val ENV_UNKNOWN_C = "EnvUnknownC"
        const val ENV_UNKNOWN_D = "EnvUnknownD"
        const val ENV_UNKNOWN_E = "EnvUnknownE"
        const val ENV_CARD_SERIAL = "EnvCardSerial"
        const val HOLDER_ID_NUMBER = "HolderIdNumber"
        const val HOLDER_UNKNOWN_A = "HolderUnknownA"
        const val HOLDER_UNKNOWN_B = "HolderUnknownB"
        const val HOLDER_UNKNOWN_C = "HolderUnknownC"
        const val HOLDER_UNKNOWN_D = "HolderUnknownD"
        const val CONTRACTS_PROVIDER = "ContractsProvider"
        const val CONTRACTS_POINTER = "ContractsPointer"
        const val CONTRACTS_TARIFF = "ContractsTariff"
        const val CONTRACTS_UNKNOWN_A = "ContractsUnknownA"
        const val CONTRACTS_UNKNOWN_B = "ContractsUnknownB"
        const val CONTRACTS_NETWORK_ID = "ContractsNetworkId"
    }
}
