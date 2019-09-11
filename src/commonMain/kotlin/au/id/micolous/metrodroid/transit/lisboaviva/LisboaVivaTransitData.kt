/*
 * LisboaVivaTransitData.kt
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

package au.id.micolous.metrodroid.transit.lisboaviva

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

// Reference: https://github.com/L1L1/cardpeek/blob/master/dot_cardpeek_dir/scripts/calypso/c131.lua
@Parcelize
class LisboaVivaTransitData (private val capsule: Calypso1545TransitDataCapsule,
                             private val holderName: String?,
                             private val tagId: Long?):
        Calypso1545TransitData(capsule) {

    override val cardName: String
        get() = NAME

    override val info: List<ListItem>?
        get() = super.info.orEmpty() + (
                if (!Preferences.hideCardNumbers)
                    listOf(ListItem(R.string.lisboaviva_engraved_serial, tagId?.toString()))
                else
                    emptyList()) +
                (if (holderName != null && holderName.isNotEmpty() && !Preferences.hideCardNumbers)
                    listOf(ListItem(R.string.card_holders_name, holderName))
                else
                    emptyList())

    override val lookup get() = LisboaVivaLookup

    companion object {
        private const val COUNTRY_PORTUGAL = 0x131
        private const val NAME = "Viva"

        private val CARD_INFO = CardInfo(
                name = "Lisboa Viva", // The card is literally branded like this.
                imageId = R.drawable.lisboaviva,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                locationId = R.string.location_lisbon,
                cardType = CardType.ISO7816,
                region = TransitRegion.PORTUGAL,
                preview = true)

        private fun parse(card: CalypsoApplication) =
                LisboaVivaTransitData(
                        capsule = Calypso1545TransitData.parse(
                                card, TICKETING_ENV_FIELDS, null, getSerial(card),
                                { data, ctr, _, _ -> LisboaVivaSubscription(data, ctr) },
                                { data -> LisboaVivaTransaction(data) }),
                        tagId = card.getFile(CalypsoApplication.File.ICC)?.getRecord(1)
                                ?.byteArrayToLong(16, 4),
                        holderName = card.getFile(CalypsoApplication.File.ID)?.getRecord(1)?.readLatin1())

        private val TICKETING_ENV_FIELDS = En1545Container(
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_A, 13),
                En1545FixedInteger("EnvNetworkCountry", 12),
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_B, 5),
                En1545FixedInteger("CardSerialPrefix", 8),
                En1545FixedInteger(En1545TransitData.ENV_CARD_SERIAL, 24),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_ISSUE),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_C, 15),
                En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
                En1545FixedHex(En1545TransitData.ENV_UNKNOWN_D, 95)
        )

        private fun getSerial(card: CalypsoApplication): String {
            val tenv = card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)?.
                    getRecord(1)!!
            return NumberUtils.zeroPad(tenv.getBitsFromBuffer(30, 8), 3) + " " +
                    NumberUtils.zeroPad(tenv.getBitsFromBuffer(38, 24), 9)
        }

        val FACTORY: CalypsoCardTransitFactory = object : CalypsoCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: CalypsoApplication) = TransitIdentity(NAME, getSerial(card))

            override fun check(tenv: ImmutableByteArray) = try {
                COUNTRY_PORTUGAL == tenv.getBitsFromBuffer(13, 12)
            } catch (e: Exception) {
                false
            }

            override fun getCardInfo(tenv: ImmutableByteArray) = CARD_INFO

            override fun parseTransitData(card: CalypsoApplication) = parse(card)
        }
    }
}
