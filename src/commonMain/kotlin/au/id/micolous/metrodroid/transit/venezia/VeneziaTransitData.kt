/*
 * RavKavTransitData.java
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

package au.id.micolous.metrodroid.transit.venezia

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Duration
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class VeneziaTransitData (val calypso: Calypso1545TransitDataCapsule) : Calypso1545TransitData(calypso) {

    private constructor(card: CalypsoApplication) :
            this(calypso = Calypso1545TransitData.parse(
                    card = card,
                    ticketEnvHolderFields = TICKETING_ENV_FIELDS,
                    // Venezia Unica doesn't use contract list
                    contractListFields = null,
                    createTrip = VeneziaTransactionCalypso.Companion::parse,
                    createSpecialEvent = { null },
                    createSubscription = { data, ctr, _, _ -> VeneziaSubscription.parse(data, ctr) },
                    serial = getSerial(card)))

    override val cardName get() = NAME

    override val lookup get() = VeneziaLookup

    private val profileNumber get() = mTicketEnvParsed.getIntOrZero("HolderProfileNumber")

    private val profileDescription: String
        get() = when (profileNumber) {
            117 -> Localizer.localizeString(R.string.venezia_profile_normal)
            else -> Localizer.localizeString(R.string.unknown_format, profileNumber)
        }

    override val info get(): List<ListItem> = super.info.orEmpty()+ listOf(
                ListItem(Localizer.localizeString(R.string.venezia_profile), profileDescription)
        )

    companion object {
        private const val NAME = "Venezia Unica"

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_venezia,
                cardType = CardType.ISO7816,
                preview = true)

        private val TICKETING_ENV_FIELDS = En1545Container(
                En1545FixedHex(ENV_UNKNOWN_A, 49),
                En1545FixedInteger.datePacked(ENV_APPLICATION_VALIDITY_END),
                En1545FixedInteger("HolderProfileNumber", 8),
                En1545FixedHex(ENV_UNKNOWN_B, 2),
                En1545FixedInteger.datePacked(HOLDER_PROFILE)
                // Rest is zero-filled
        )

        private fun getSerial(card: CalypsoApplication): String? {
            val iccFile = card.getFile(CalypsoApplication.File.ICC) ?: return null
            val iccRecord = iccFile.getRecord(1) ?: return null

            return iccRecord.byteArrayToLong(9, 4).toString()
        }

        val FACTORY: CalypsoCardTransitFactory = object : CalypsoCardTransitFactory {
            override val allCards get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: CalypsoApplication) = TransitIdentity(NAME, getSerial(card))

            override fun check(tenv: ImmutableByteArray) = try {
                tenv.byteArrayToInt(0, 4) == 0x7d0
            } catch (e: Exception) {
                false
            }

            override fun getCardInfo(tenv: ImmutableByteArray) = CARD_INFO

            override fun parseTransitData(card: CalypsoApplication) = VeneziaTransitData(card)
        }
    }
}
