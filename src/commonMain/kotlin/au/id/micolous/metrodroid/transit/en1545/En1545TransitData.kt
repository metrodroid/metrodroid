/*
 * IntercodeTransitData.java
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

abstract class En1545TransitData : TransitData {

    protected val mTicketEnvParsed: En1545Parsed

    override val info: List<ListItem>?
        get() {
            val li = mutableListOf<ListItem>()
            val tz = lookup.timeZone
            if (mTicketEnvParsed.contains(ENV_NETWORK_ID))
                li.add(ListItem(R.string.en1545_network_id,
                        mTicketEnvParsed.getIntOrZero(ENV_NETWORK_ID).toString(16)))
            if (mTicketEnvParsed.getIntOrZero(En1545FixedInteger.dateName(ENV_APPLICATION_VALIDITY_END)) != 0)
                li.add(ListItem(R.string.expiry_date,
                        mTicketEnvParsed.getTimeStampString(ENV_APPLICATION_VALIDITY_END, tz)))
            if (mTicketEnvParsed.getIntOrZero(HOLDER_BIRTH_DATE) != 0)
                li.add(ListItem(R.string.date_of_birth,
                        TimestampFormatter.longDateFormat(En1545FixedInteger.parseBCDDate(
                                mTicketEnvParsed.getIntOrZero(HOLDER_BIRTH_DATE)))))
            if (mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_ISSUER_ID) != 0)
                li.add(ListItem(R.string.card_issuer,
                        lookup.getAgencyName(mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_ISSUER_ID), false)))
            if (mTicketEnvParsed.getIntOrZero(En1545FixedInteger.dateName(ENV_APPLICATION_ISSUE)) != 0)
                li.add(ListItem(R.string.issue_date, mTicketEnvParsed.getTimeStampString(ENV_APPLICATION_ISSUE, tz)))

            if (mTicketEnvParsed.getIntOrZero(En1545FixedInteger.dateName(HOLDER_PROFILE)) != 0)
                li.add(ListItem(R.string.en1545_card_expiry_date_profile, mTicketEnvParsed.getTimeStampString(HOLDER_PROFILE, tz)))

            if (mTicketEnvParsed.getIntOrZero(HOLDER_POSTAL_CODE) != 0)
                li.add(ListItem(R.string.postal_code,
                        mTicketEnvParsed.getIntOrZero(HOLDER_POSTAL_CODE).toString()))

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
                                HOLDER_BIRTH_DATE,
                                ENV_APPLICATION_ISSUER_ID,
                                En1545FixedInteger.datePackedName(ENV_APPLICATION_ISSUE),
                                En1545FixedInteger.dateName(ENV_APPLICATION_ISSUE),
                                En1545FixedInteger.datePackedName(HOLDER_PROFILE),
                                En1545FixedInteger.dateName(HOLDER_PROFILE),
                                HOLDER_POSTAL_CODE,
                                ENV_CARD_SERIAL
                        )
                        else -> setOf()
                    })

    companion object {
        const val ENV_NETWORK_ID = "EnvNetworkId"
        const val ENV_VERSION_NUMBER = "EnvVersionNumber"
        const val HOLDER_BIRTH_DATE = "HolderBirthDate"
        const val ENV_APPLICATION_VALIDITY_END = "EnvApplicationValidityEnd"
        const val ENV_APPLICATION_ISSUER_ID = "EnvApplicationIssuerId"
        const val ENV_APPLICATION_ISSUE = "EnvApplicationIssue"
        const val HOLDER_PROFILE = "HolderProfile"
        const val HOLDER_POSTAL_CODE = "HolderPostalCode"
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
