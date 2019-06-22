/*
 * RavKavTransitData.kt
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

package au.id.micolous.metrodroid.transit.ravkav

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory
import au.id.micolous.metrodroid.card.iso7816.ISO7816Data.TAG_DISCRETIONARY_DATA
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV.findBERTLV
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

// Reference: https://github.com/L1L1/cardpeek/blob/master/dot_cardpeek_dir/scripts/calypso/c376n3.lua
// supplemented with personal experimentation
@Parcelize
class RavKavTransitData (val capsule: Calypso1545TransitDataCapsule): Calypso1545TransitData(capsule) {

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_ravkav)

    override val info: List<ListItem>?
        get() = listOf(
                if (mTicketEnvParsed.getIntOrZero(HOLDER_ID_NUMBER) == 0) {
                    ListItem(R.string.card_type, R.string.card_type_anonymous)
                } else {
                    ListItem(R.string.card_type, R.string.card_type_personal)
                }) + super.info.orEmpty()

    private constructor(card: CalypsoApplication) : this(parse(
            card, TICKETING_ENV_FIELDS, null, getSerial(card),
            { data, counter, _, _ -> RavKavSubscription(data, counter) },
            { data -> createTrip(data) }))

    override val lookup get(): En1545Lookup = RavKavLookup

    companion object {
        // 376 = Israel
        private const val RAVKAV_NETWORK_ID_A = 0x37602
        private const val RAVKAV_NETWORK_ID_B = 0x37603

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.ravkav_card,
                name = R.string.card_name_ravkav,
                locationId = R.string.location_israel,
                cardType = CardType.ISO7816,
                preview = true
        )

        private val TICKETING_ENV_FIELDS = En1545Container(
                En1545FixedInteger(ENV_VERSION_NUMBER, 3),
                En1545FixedInteger(ENV_NETWORK_ID, 20),
                En1545FixedInteger(ENV_UNKNOWN_A, 26),
                En1545FixedInteger.date(ENV_APPLICATION_ISSUE),
                En1545FixedInteger.date(ENV_APPLICATION_VALIDITY_END),
                En1545FixedInteger("PayMethod", 3),
                En1545FixedInteger(HOLDER_BIRTH_DATE, 32),
                En1545FixedHex(ENV_UNKNOWN_B, 44),
                En1545FixedInteger(HOLDER_ID_NUMBER, 30)
        )

        private fun getSerial(card: CalypsoApplication): String? {
            val bf0c = findBERTLV(card.appProprietaryBerTlv ?: return null, TAG_DISCRETIONARY_DATA, true) ?: return null
            val c7 = findBERTLV(bf0c, "c7", false)
            return c7?.byteArrayToLong(4, 4).toString()
        }

        val FACTORY: CalypsoCardTransitFactory = object : CalypsoCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: CalypsoApplication) =
                    TransitIdentity(Localizer.localizeString(R.string.card_name_ravkav), getSerial(card))

            override fun check(tenv: ImmutableByteArray) = try {
                tenv.getBitsFromBuffer(3, 20) in listOf(RAVKAV_NETWORK_ID_A, RAVKAV_NETWORK_ID_B)
            } catch (e: Exception) {
                false
            }

            override fun getCardInfo(tenv: ImmutableByteArray) = CARD_INFO

            override fun parseTransitData(card: CalypsoApplication) = RavKavTransitData(card)
        }

        private fun createTrip(data: ImmutableByteArray): En1545Transaction? {
            val t = RavKavTransaction(data)
            return if (t.shouldBeDropped()) null else t
        }
    }
}
