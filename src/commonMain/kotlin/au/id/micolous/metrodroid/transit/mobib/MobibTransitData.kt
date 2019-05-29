/*
 * MobibTransitData.kt
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

package au.id.micolous.metrodroid.transit.mobib

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

/*
 * Reference:
 * - https://github.com/zoobab/mobib-extractor
 */
@Parcelize
class MobibTransitData (private val extHolderParsed: En1545Parsed,
                        private val purchase: Int,
                        private val totalTrips: Int,
                        private val capsule: Calypso1545TransitDataCapsule): Calypso1545TransitData(capsule) {

    override val info: List<ListItem>?
        get() {
            val li = mutableListOf<ListItem>()
            if (purchase != 0)
                li.add(ListItem(R.string.purchase_date,
                        TimestampFormatter.longDateFormat(En1545FixedInteger.parseDate(purchase, TZ)!!)))
            li.add(ListItem(R.string.transaction_counter, totalTrips.toString()))
            val gender = extHolderParsed.getIntOrZero(EXT_HOLDER_GENDER)
            if (gender == 0) {
                li.add(ListItem(R.string.card_type, R.string.card_type_anonymous))
            } else {
                li.add(ListItem(R.string.card_type, R.string.card_type_personal))
            }
            if (gender != 0 && !Preferences.hideCardNumbers
                    && !Preferences.obfuscateTripDates) {
                li.add(ListItem(R.string.card_holders_name,
                        extHolderParsed.getString(EXT_HOLDER_NAME)))
                when (gender) {
                    1 -> li.add(ListItem(R.string.gender,
                            R.string.gender_male))
                    2 -> li.add(ListItem(R.string.gender,
                            R.string.gender_female))
                    else -> li.add(ListItem(R.string.gender,
                            gender.toString(16)))
                }
            }
            li.addAll(super.info.orEmpty())
            return li
        }

    override val cardName: String
        get() = NAME

    override val lookup get() = MobibLookup.instance

    companion object {
        // 56 = Belgium
        private const val MOBIB_NETWORK_ID = 0x56001
        const val NAME = "Mobib"
        private const val EXT_HOLDER_NAME = "ExtHolderName"

        private val CARD_INFO = CardInfo(
                name = MobibTransitData.NAME,
                cardType = CardType.ISO7816,
                imageId = R.drawable.mobib_card,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                locationId = R.string.location_brussels)

        val TZ = MetroTimeZone.BRUXELLES

        private val ticketEnvFields = En1545Container(
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_A, 13),
                En1545FixedInteger(En1545TransitData.ENV_NETWORK_ID, 24),
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_B, 9),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_C, 6),
                En1545FixedInteger(En1545TransitData.HOLDER_BIRTH_DATE, 32),
                En1545FixedHex(En1545TransitData.ENV_CARD_SERIAL, 76),
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_D, 5),
                En1545FixedInteger(En1545TransitData.HOLDER_POSTAL_CODE, 14),
                En1545FixedHex(En1545TransitData.ENV_UNKNOWN_E, 34)
        )
        private const val EXT_HOLDER_GENDER = "ExtHolderGender"
        private const val EXT_HOLDER_DATE_OF_BIRTH = "ExtHolderDateOfBirth"
        private const val EXT_HOLDER_CARD_SERIAL = "ExtHolderCardSerial"
        private const val EXT_HOLDER_UNKNOWN_A = "ExtHolderUnknownA"
        private const val EXT_HOLDER_UNKNOWN_B = "ExtHolderUnknownB"
        private const val EXT_HOLDER_UNKNOWN_C = "ExtHolderUnknownC"
        private const val EXT_HOLDER_UNKNOWN_D = "ExtHolderUnknownD"

        private val extHolderFields = En1545Container(
                En1545FixedInteger(EXT_HOLDER_UNKNOWN_A, 18),
                En1545FixedHex(EXT_HOLDER_CARD_SERIAL, 76),
                En1545FixedInteger(EXT_HOLDER_UNKNOWN_B, 16),
                En1545FixedHex(EXT_HOLDER_UNKNOWN_C, 58),
                En1545FixedInteger(EXT_HOLDER_DATE_OF_BIRTH, 32),
                En1545FixedInteger(EXT_HOLDER_GENDER, 2),
                En1545FixedInteger(EXT_HOLDER_UNKNOWN_D, 3),
                En1545FixedString(EXT_HOLDER_NAME, 259)
        )

        private val contractListFields = En1545Repeat(4,
                En1545Container(
                        En1545FixedHex(En1545TransitData.CONTRACTS_UNKNOWN_A, 18),
                        En1545FixedInteger(En1545TransitData.CONTRACTS_POINTER, 5),
                        En1545FixedHex(En1545TransitData.CONTRACTS_UNKNOWN_B, 16)
                )
        )

        private fun getSerial(card: CalypsoApplication): String {
            val holder = card.getFile(CalypsoApplication.File.HOLDER_EXTENDED)?.getRecord(1)!!
            return NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(18, 24)),6) + " / " +
                    NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(42, 24)), 6) +
                    NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(66, 16)), 4) +
                    NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(82, 8)), 2) + " / " +
                    NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(90, 4)).toString()
        }

        private fun parse(card: CalypsoApplication) : MobibTransitData {
            val capsule = Calypso1545TransitData.parse(card, ticketEnvFields,
                contractListFields, getSerial(card),
                { data, counter, _, _ -> MobibSubscription(data, counter) },
                { data -> MobibTransaction(data) })
            val holderFile = card.getFile(CalypsoApplication.File.HOLDER_EXTENDED)
            val holder = holderFile!!.getRecord(1)!!.plus(
                    holderFile.getRecord(2)!!)
            val extHolderParsed = En1545Parser.parse(holder, extHolderFields)
            val purchase = card.getFile(CalypsoApplication.File.EP_LOAD_LOG)!!
                    .getRecord(1)!!.getBitsFromBuffer(
                    2, 14)
            val totalTrips = card.getFile(CalypsoApplication.File.TICKETING_LOG)?.recordList?.map {
                record -> record.getBitsFromBuffer(17 * 8 + 3, 23) }?.max() ?: 0
            return MobibTransitData(capsule = capsule, totalTrips = totalTrips,
                    purchase = purchase, extHolderParsed = extHolderParsed)
        }

        val FACTORY: CalypsoCardTransitFactory = object : CalypsoCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: CalypsoApplication) = TransitIdentity(NAME, getSerial(card))

            override fun check(tenv: ImmutableByteArray) = try {
                    MOBIB_NETWORK_ID == tenv.getBitsFromBuffer(13, 24)
                } catch (e: Exception) {
                    false
                }

            override fun getCardInfo(tenv: ImmutableByteArray) = CARD_INFO

            override fun parseTransitData(card: CalypsoApplication) = parse(card)
        }
    }
}
