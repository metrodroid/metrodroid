/*
 * OpusTransitData.kt
 *
 * Copyright 2018 Etienne Dubeau
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

package au.id.micolous.metrodroid.transit.opus

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.transit.intercode.IntercodeTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class OpusTransitData (val capsule: Calypso1545TransitDataCapsule): Calypso1545TransitData(capsule) {

    override val cardName: String
        get() = NAME

    private constructor(card: CalypsoApplication) : this(Calypso1545TransitData.parse(
            card, ticketEnvFields, contractListFields, getSerial(card),
            { data, counter, _, _ -> if (counter == null) null else OpusSubscription(data, counter) },
            { data -> OpusTransaction(data) }, null,
            // Contracts 2 is a copy of contract list on opus
            card.getFile(CalypsoApplication.File.TICKETING_CONTRACTS_1)?.recordList.orEmpty()))

    override val lookup get() = OpusLookup

    companion object {
        // 124 = Canada
        private const val OPUS_NETWORK_ID = 0x124001
        private const val NAME = "Opus"

        private val contractListFields = En1545Repeat(4,
                En1545Bitmap(
                        En1545FixedInteger(En1545TransitData.CONTRACTS_PROVIDER, 8),
                        En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF, 16),
                        En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_A, 4),
                        En1545FixedInteger(En1545TransitData.CONTRACTS_POINTER, 5)
                )
        )

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.opus_card,
                name = OpusTransitData.NAME,
                locationId = R.string.location_quebec,
                cardType = CardType.ISO7816,
                region = TransitRegion.CANADA,
                preview = true)

        private val ticketEnvFields = En1545Container(
                IntercodeTransitData.TICKET_ENV_FIELDS,
                En1545Bitmap(
                        En1545Container(
                                En1545FixedInteger(En1545TransitData.HOLDER_UNKNOWN_A, 3),
                                En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
                                En1545FixedInteger(En1545TransitData.HOLDER_UNKNOWN_B, 13),
                                En1545FixedInteger.date(En1545TransitData.HOLDER_PROFILE),
                                En1545FixedInteger(En1545TransitData.HOLDER_UNKNOWN_C, 8)
                        ),
                        // Possibly part of HolderUnknownB or HolderUnknownC
                        En1545FixedInteger(En1545TransitData.HOLDER_UNKNOWN_D, 8)
                )
        )

        val FACTORY: CalypsoCardTransitFactory = object : CalypsoCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: CalypsoApplication) = TransitIdentity(NAME, getSerial(card))

            override fun check(tenv: ImmutableByteArray) = try {
                    OPUS_NETWORK_ID == tenv.getBitsFromBuffer(13, 24)
                } catch (e: Exception) {
                    false
                }

            override fun parseTransitData(card: CalypsoApplication) = OpusTransitData(card)

            override fun getCardInfo(tenv: ImmutableByteArray) = CARD_INFO
        }

        private fun getSerial(card: CalypsoApplication): String? {
            val iccData = card.getFile(CalypsoApplication.File.ICC)?.getRecord(1) ?: return null

            if (iccData.byteArrayToLong(16, 4) != 0L) {
                return iccData.byteArrayToLong(16, 4).toString()
            }

            if (iccData.byteArrayToLong(0, 4) != 0L) {
                return iccData.byteArrayToLong(0, 4).toString()
            }

            return null
        }
    }
}
