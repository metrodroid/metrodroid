/*
 * PisaTransitData.kt
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.transit.pisa

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class PisaTransitData(val calypso: Calypso1545TransitDataCapsule) : Calypso1545TransitData(calypso) {

    private constructor(card: CalypsoApplication) :
            this(calypso = parse(
                    card = card,
                    ticketEnvHolderFields = TICKETING_ENV_FIELDS,
                    serial = getSerial(card),
                    contractListFields = null,
                    createTrip = PisaTransaction.Companion::parse,
                    createSpecialEvent = PisaSpecialEvent.Companion::parse,
                    createSubscription = {data, ctr, _, _ -> PisaSubscription.parse(data, ctr)}))

    override val cardName get() = NAME

    override val lookup get() = PisaLookup

    companion object {
        private const val NAME = "Carta Mobile"

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_pisa,
                cardType = CardType.ISO7816,
                imageId = R.drawable.cartamobile,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                preview = true)

        private val TICKETING_ENV_FIELDS = En1545Container(
                En1545FixedInteger(ENV_VERSION_NUMBER, 5),
                En1545FixedInteger(ENV_NETWORK_ID, 24),
                En1545FixedHex(ENV_UNKNOWN_A, 44),
                En1545FixedInteger.date(ENV_APPLICATION_ISSUE),
                En1545FixedInteger.date(ENV_APPLICATION_VALIDITY_END),
                En1545FixedInteger.dateBCD(HOLDER_BIRTH_DATE)
                // Remainder: zero-filled
        )

        private fun getSerial(card: CalypsoApplication): String? {
            val envFile = card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT) ?: return null
            val envRecord = envFile.getRecord(2) ?: return null

            return envRecord.readASCII()
        }

        const val PISA_NETWORK_ID = 0x380100
        val FACTORY: CalypsoCardTransitFactory = object : CalypsoCardTransitFactory {
            override val allCards get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: CalypsoApplication) = TransitIdentity(NAME, getSerial(card))

            override fun check(tenv: ImmutableByteArray) = try {
                tenv.getBitsFromBuffer(5, 24) == PISA_NETWORK_ID
            } catch (e: Exception) {
                false
            }

            override fun getCardInfo(tenv: ImmutableByteArray) = CARD_INFO

            override fun parseTransitData(card: CalypsoApplication) = PisaTransitData(card)
        }
    }
}
